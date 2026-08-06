package dev.brunovasconcellos.vbank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String version = "1.0.0";
    private final Jwt jwt = new Jwt();
    private final Cookie cookie = new Cookie();
    private final AdminBootstrap adminBootstrap = new AdminBootstrap();

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Jwt getJwt() { return jwt; }
    public Cookie getCookie() { return cookie; }
    public AdminBootstrap getAdminBootstrap() { return adminBootstrap; }

    public static class Jwt {
        private String secret;
        private long accessExpirationMinutes = 15;
        private long refreshExpirationDays = 7;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getAccessExpirationMinutes() { return accessExpirationMinutes; }
        public void setAccessExpirationMinutes(long value) { this.accessExpirationMinutes = value; }
        public long getRefreshExpirationDays() { return refreshExpirationDays; }
        public void setRefreshExpirationDays(long value) { this.refreshExpirationDays = value; }
    }

    public static class Cookie {
        private boolean secure;
        private String domain;
        public boolean isSecure() { return secure; }
        public void setSecure(boolean secure) { this.secure = secure; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
    }

    public static class AdminBootstrap {
        private boolean enabled;
        private String name;
        private String email;
        private String username;
        private String password;
        private String pin;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getPin() { return pin; }
        public void setPin(String pin) { this.pin = pin; }
    }
}

