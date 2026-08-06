package dev.brunovasconcellos.vbank.config;

import dev.brunovasconcellos.vbank.api.ApiDtos;
import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.repository.RefreshTokenRepository;
import dev.brunovasconcellos.vbank.repository.UserRepository;
import dev.brunovasconcellos.vbank.service.AuditService;
import dev.brunovasconcellos.vbank.service.AuthService;
import dev.brunovasconcellos.vbank.service.DomainNormalizer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {
    private final AppProperties properties;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AdminBootstrapRunner(AppProperties properties, UserRepository userRepository,
                                RefreshTokenRepository refreshTokenRepository, AuthService authService,
                                PasswordEncoder passwordEncoder, AuditService auditService) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.AdminBootstrap config = properties.getAdminBootstrap();
        if (!config.isEnabled()) return;
        String username = DomainNormalizer.username(config.getUsername());
        String email = DomainNormalizer.email(config.getEmail());
        var byUsername = userRepository.findByUsername(username);
        var byEmail = userRepository.findByEmail(email);
        if (byUsername.isPresent() || byEmail.isPresent()) {
            if (byUsername.isPresent() && byEmail.isPresent()
                    && byUsername.get().getId().equals(byEmail.get().getId())) return;
            throw new IllegalStateException("O bootstrap admin conflita com um e-mail ou username já existente.");
        }
        var session = authService.register(new ApiDtos.RegisterRequest(config.getName(), config.getEmail(), username,
                config.getPassword(), config.getPassword(), true), "bootstrap administrativo");
        var user = userRepository.findByUsername(username).orElseThrow();
        user.addRole(Enums.Role.ADMIN);
        user.setPinHash(passwordEncoder.encode(config.getPin()));
        refreshTokenRepository.revokeAll(user.getId(), Instant.now());
        auditService.record(user, "ADMIN_BOOTSTRAP", "SUCCESS", "USER", user.getId().toString(),
                "desative ADMIN_BOOTSTRAP_ENABLED após o primeiro deploy");
        authService.logout(session.refreshToken());
    }
}
