package dev.brunovasconcellos.vbank.api;

import dev.brunovasconcellos.vbank.config.AppProperties;
import dev.brunovasconcellos.vbank.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import dev.brunovasconcellos.vbank.security.CurrentUser;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "vbank_refresh";
    private final AuthService authService;
    private final AppProperties properties;

    public AuthController(AuthService authService, AppProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/register")
    ResponseEntity<ApiDtos.TokenResponse> register(@Valid @RequestBody ApiDtos.RegisterRequest request,
                                                    HttpServletRequest httpRequest) {
        return withCookie(authService.register(request, device(httpRequest)));
    }

    @PostMapping("/login")
    ResponseEntity<ApiDtos.TokenResponse> login(@Valid @RequestBody ApiDtos.LoginRequest request,
                                                 HttpServletRequest httpRequest) {
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        String fingerprint = forwarded == null ? httpRequest.getRemoteAddr() : forwarded.split(",")[0].trim();
        return withCookie(authService.login(request, fingerprint, device(httpRequest)));
    }

    @PostMapping("/refresh")
    ResponseEntity<ApiDtos.TokenResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String token,
                                                   HttpServletRequest request) {
        return withCookie(authService.refresh(token, device(request)));
    }

    @PostMapping("/logout")
    ResponseEntity<ApiDtos.SessionResponse> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
        authService.logout(token);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
                .body(new ApiDtos.SessionResponse("Sessão encerrada."));
    }

    @PostMapping("/logout-all")
    ResponseEntity<ApiDtos.SessionResponse> logoutAll(Authentication authentication) {
        authService.logoutAll(CurrentUser.id(authentication));
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
                .body(new ApiDtos.SessionResponse("Todas as sessões foram encerradas."));
    }

    private ResponseEntity<ApiDtos.TokenResponse> withCookie(AuthService.SessionTokens session) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(session).toString()).body(session.response());
    }

    private ResponseCookie cookie(AuthService.SessionTokens session) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_COOKIE, session.refreshToken())
                .httpOnly(true).secure(properties.getCookie().isSecure()).sameSite("Lax").path("/api/auth")
                .maxAge(Duration.between(java.time.Instant.now(), session.refreshExpiresAt()));
        if (properties.getCookie().getDomain() != null && !properties.getCookie().getDomain().isBlank()) {
            builder.domain(properties.getCookie().getDomain());
        }
        return builder.build();
    }

    private ResponseCookie expiredCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "").httpOnly(true).secure(properties.getCookie().isSecure())
                .sameSite("Lax").path("/api/auth").maxAge(Duration.ZERO).build();
    }

    private String device(HttpServletRequest request) { return request.getHeader("User-Agent"); }
}
