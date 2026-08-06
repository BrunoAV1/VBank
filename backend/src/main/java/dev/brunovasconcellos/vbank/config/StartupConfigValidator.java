package dev.brunovasconcellos.vbank.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class StartupConfigValidator {
    private final AppProperties properties;
    private final Environment environment;

    public StartupConfigValidator(AppProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        String secret = properties.getJwt().getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT_SECRET é obrigatório. Gere um segredo aleatório com pelo menos 32 bytes.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET é muito curto. Use pelo menos 32 bytes aleatórios.");
        }
        String url = environment.getProperty("spring.datasource.url");
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException("SPRING_DATASOURCE_URL é obrigatória e deve apontar para o PostgreSQL.");
        }
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("SPRING_DATASOURCE_URL deve ser uma URL JDBC do PostgreSQL (jdbc:postgresql://...).");
        }
        if (!StringUtils.hasText(environment.getProperty("spring.datasource.username"))) {
            throw new IllegalStateException("SPRING_DATASOURCE_USERNAME é obrigatório.");
        }
        if (!StringUtils.hasText(environment.getProperty("spring.datasource.password"))) {
            throw new IllegalStateException("SPRING_DATASOURCE_PASSWORD é obrigatório e não pode ser vazio.");
        }
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (prod && (Arrays.asList(environment.getActiveProfiles()).contains("dev")
                || Arrays.asList(environment.getActiveProfiles()).contains("test"))) {
            throw new IllegalStateException("O perfil prod não pode ser combinado com dev ou test.");
        }
        if (prod && !properties.getCookie().isSecure()) {
            throw new IllegalStateException("COOKIE_SECURE deve ser true no perfil prod.");
        }
        if (prod && !(url.contains("sslmode=require") || url.contains("ssl=true"))) {
            throw new IllegalStateException("A URL JDBC de produção deve exigir SSL (sslmode=require).");
        }
        if (properties.getJwt().getAccessExpirationMinutes() < 1 || properties.getJwt().getRefreshExpirationDays() < 1) {
            throw new IllegalStateException("As expirações JWT devem ser positivas.");
        }
        AppProperties.AdminBootstrap admin = properties.getAdminBootstrap();
        if (admin.isEnabled()) {
            if (!StringUtils.hasText(admin.getName()) || !StringUtils.hasText(admin.getEmail())
                    || !StringUtils.hasText(admin.getUsername()) || admin.getPassword() == null
                    || !admin.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                    || !admin.getUsername().matches("^@?[A-Za-z0-9._-]{3,40}$")
                    || !admin.getPassword().matches(dev.brunovasconcellos.vbank.api.ApiDtos.PASSWORD_PATTERN)
                    || admin.getPin() == null || !admin.getPin().matches(dev.brunovasconcellos.vbank.api.ApiDtos.PIN_PATTERN)) {
                throw new IllegalStateException("Bootstrap admin habilitado com campos ausentes, senha fraca ou PIN inválido.");
            }
        }
    }
}
