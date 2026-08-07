package main

import (
	"context"
	"errors"
	"fmt"
	"log"
	"net"
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"
	"os/exec"
	"os/signal"
	"strings"
	"syscall"
	"time"
)

const (
	defaultExternalPort = "8080"
	defaultBackendPort  = "18080"
	backendWaitTimeout  = 90 * time.Second
)

func main() {
	externalPort := envOrDefault("PORT", defaultExternalPort)
	backendPort := envOrDefault("BACKEND_PORT", defaultBackendPort)
	if externalPort == backendPort {
		log.Fatalf("PORT and BACKEND_PORT must be different (both are %s)", externalPort)
	}

	listener, err := net.Listen("tcp", ":"+externalPort)
	if err != nil {
		log.Fatalf("could not listen on PORT=%s: %v", externalPort, err)
	}
	log.Printf("startup proxy listening on PORT=%s", externalPort)

	java := exec.Command(
		envOrDefault("JAVA_BIN", "/opt/java/openjdk/bin/java"),
		"-XX:MaxRAMPercentage=75",
		"-XX:+UseSerialGC",
		"-XX:TieredStopAtLevel=1",
		"-Djava.security.egd=file:/dev/urandom",
		"-jar",
		envOrDefault("JAVA_JAR", "/app/vbank.jar"),
	)
	java.Env = replaceEnv(os.Environ(), "PORT", backendPort)
	java.Stdout = os.Stdout
	java.Stderr = os.Stderr

	if err := java.Start(); err != nil {
		_ = listener.Close()
		log.Fatalf("could not start Spring Boot: %v", err)
	}
	log.Printf("Spring Boot starting on internal port %s", backendPort)

	target, err := url.Parse("http://127.0.0.1:" + backendPort)
	if err != nil {
		_ = java.Process.Kill()
		_ = listener.Close()
		log.Fatalf("invalid backend address: %v", err)
	}

	proxy := httputil.NewSingleHostReverseProxy(target)
	proxy.ErrorHandler = func(response http.ResponseWriter, request *http.Request, proxyErr error) {
		log.Printf("proxy request failed: %v", proxyErr)
		http.Error(response, "backend temporarily unavailable", http.StatusServiceUnavailable)
	}

	backendAddress := net.JoinHostPort("127.0.0.1", backendPort)
	server := &http.Server{
		Handler: http.HandlerFunc(func(response http.ResponseWriter, request *http.Request) {
			ctx, cancel := context.WithTimeout(request.Context(), backendWaitTimeout)
			defer cancel()
			if err := waitForBackend(ctx, backendAddress); err != nil {
				log.Printf("backend did not become ready: %v", err)
				http.Error(response, "application is still starting", http.StatusServiceUnavailable)
				return
			}
			proxy.ServeHTTP(response, request)
		}),
		ReadHeaderTimeout: 10 * time.Second,
	}

	javaDone := make(chan error, 1)
	go func() { javaDone <- java.Wait() }()

	serverDone := make(chan error, 1)
	go func() { serverDone <- server.Serve(listener) }()

	signalContext, stopSignals := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stopSignals()

	javaExited := false
	select {
	case <-signalContext.Done():
		log.Printf("shutdown signal received")
	case err := <-javaDone:
		javaExited = true
		log.Printf("Spring Boot exited: %v", err)
	case err := <-serverDone:
		if !errors.Is(err, http.ErrServerClosed) {
			log.Printf("startup proxy exited: %v", err)
		}
	}

	shutdownContext, cancelShutdown := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancelShutdown()
	_ = server.Shutdown(shutdownContext)

	if !javaExited && java.Process != nil {
		_ = java.Process.Signal(syscall.SIGTERM)
		select {
		case <-javaDone:
		case <-shutdownContext.Done():
			_ = java.Process.Kill()
		}
	}
}

func waitForBackend(ctx context.Context, address string) error {
	dialer := net.Dialer{Timeout: 250 * time.Millisecond}
	ticker := time.NewTicker(100 * time.Millisecond)
	defer ticker.Stop()

	for {
		connection, err := dialer.DialContext(ctx, "tcp", address)
		if err == nil {
			_ = connection.Close()
			return nil
		}

		select {
		case <-ctx.Done():
			return fmt.Errorf("waiting for %s: %w", address, ctx.Err())
		case <-ticker.C:
		}
	}
}

func envOrDefault(key, fallback string) string {
	if value := strings.TrimSpace(os.Getenv(key)); value != "" {
		return value
	}
	return fallback
}

func replaceEnv(environment []string, key, value string) []string {
	prefix := key + "="
	result := make([]string, 0, len(environment)+1)
	for _, entry := range environment {
		if !strings.HasPrefix(entry, prefix) {
			result = append(result, entry)
		}
	}
	return append(result, prefix+value)
}
