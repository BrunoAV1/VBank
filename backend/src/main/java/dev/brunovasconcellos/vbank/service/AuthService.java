package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiDtos;
import dev.brunovasconcellos.vbank.api.ApiException;
import dev.brunovasconcellos.vbank.config.AppProperties;
import dev.brunovasconcellos.vbank.domain.Account;
import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.domain.LedgerEntry;
import dev.brunovasconcellos.vbank.domain.Notification;
import dev.brunovasconcellos.vbank.domain.RefreshToken;
import dev.brunovasconcellos.vbank.domain.User;
import dev.brunovasconcellos.vbank.repository.AccountRepository;
import dev.brunovasconcellos.vbank.repository.LedgerEntryRepository;
import dev.brunovasconcellos.vbank.repository.NotificationRepository;
import dev.brunovasconcellos.vbank.repository.RefreshTokenRepository;
import dev.brunovasconcellos.vbank.repository.UserRepository;
import dev.brunovasconcellos.vbank.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {
    public static final BigDecimal OPENING_BALANCE = new BigDecimal("50000.00");
    public static final BigDecimal DEFAULT_DAILY_LIMIT = new BigDecimal("10000.00");

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final NotificationRepository notificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties properties;
    private final AuditService auditService;
    private final RateLimitService rateLimitService;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository, AccountRepository accountRepository,
                       LedgerEntryRepository ledgerRepository, NotificationRepository notificationRepository,
                       RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AppProperties properties, AuditService auditService,
                       RateLimitService rateLimitService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.notificationRepository = notificationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.auditService = auditService;
        this.rateLimitService = rateLimitService;
    }

    @Transactional
    public SessionTokens register(ApiDtos.RegisterRequest request, String deviceSummary) {
        if (!request.password().equals(request.passwordConfirmation())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "A confirmação de senha não corresponde.");
        }
        String email = DomainNormalizer.email(request.email());
        String username = DomainNormalizer.username(request.username());
        if (userRepository.existsByEmail(email) || userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "VALIDATION_ERROR", "E-mail ou username já cadastrado.");
        }

        User user = userRepository.save(new User(request.fullName().trim(), email, username,
                passwordEncoder.encode(request.password()), Set.of(Enums.Role.USER)));
        Account account = accountRepository.save(new Account(user, "0001", generateAccountNumber(user.getId()),
                generateDigit(user.getId()), DEFAULT_DAILY_LIMIT));

        Account system = accountRepository.findByStatus(Enums.AccountStatus.SYSTEM)
                .orElseThrow(() -> new IllegalStateException("Conta interna do sistema ausente."));
        system = accountRepository.findAllLockedById(List.of(system.getId())).getFirst();
        if (system.getBalance().compareTo(OPENING_BALANCE) < 0) {
            throw new IllegalStateException("Reserva fictícia da conta interna insuficiente.");
        }
        system.setBalance(money(system.getBalance().subtract(OPENING_BALANCE)));
        account.setBalance(OPENING_BALANCE);
        accountRepository.saveAll(List.of(system, account));
        ledgerRepository.save(new LedgerEntry(system, null, Enums.LedgerType.DEBIT,
                Enums.LedgerCategory.OPENING_BALANCE, OPENING_BALANCE, system.getBalance(),
                "Saldo inicial destinado a nova conta de demonstração"));
        ledgerRepository.save(new LedgerEntry(account, null, Enums.LedgerType.CREDIT,
                Enums.LedgerCategory.OPENING_BALANCE, OPENING_BALANCE, account.getBalance(),
                "Saldo inicial fictício"));
        notificationRepository.save(new Notification(user, "Conta de demonstração criada",
                "Você recebeu R$ 50.000,00 de saldo fictício. Crie seu PIN antes de transferir.", "WELCOME"));
        auditService.record(user, "REGISTER", "SUCCESS", "USER", user.getId().toString(), "conta de demonstração criada");
        return createSession(user, deviceSummary);
    }

    @Transactional
    public SessionTokens login(ApiDtos.LoginRequest request, String clientFingerprint, String deviceSummary) {
        String identifier = normalizeIdentifier(request.identifier());
        rateLimitService.consume("login", clientFingerprint + ":" + identifier, 10, Duration.ofMinutes(15));
        return loginTransaction(identifier, request.password(), deviceSummary);
    }

    private SessionTokens loginTransaction(String identifier, String password, String deviceSummary) {
        User user = userRepository.findByIdentifier(identifier).orElse(null);
        if (user == null || user.getRoles().contains(Enums.Role.SYSTEM) || !passwordEncoder.matches(password, user.getPasswordHash())) {
            auditService.recordFailure(user, "LOGIN_FAILED", identifier, "credenciais inválidas");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-mail, username ou senha inválidos.");
        }
        if (user.getStatus() != Enums.UserStatus.ACTIVE) {
            auditService.recordFailure(user, "LOGIN_FAILED", user.getUsername(), "conta não ativa");
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_BLOCKED", "A conta está bloqueada ou encerrada.");
        }
        auditService.record(user, "LOGIN", "SUCCESS", "USER", user.getId().toString(), safeDevice(deviceSummary));
        return createSession(user, deviceSummary);
    }

    @Transactional
    public SessionTokens refresh(String rawRefreshToken, String deviceSummary) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Sessão expirada. Faça login novamente.");
        }
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Sessão inválida."));
        if (!stored.isUsable() || stored.getUser().getStatus() != Enums.UserStatus.ACTIVE) {
            stored.revoke();
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Sessão expirada ou revogada.");
        }
        stored.revoke();
        return createSession(stored.getUser(), deviceSummary);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenRepository.revokeAll(userId, Instant.now());
        userRepository.findById(userId).ifPresent(user -> auditService.record(user, "LOGOUT_ALL", "SUCCESS", "USER", userId.toString(), null));
    }

    @Scheduled(cron = "0 17 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        refreshTokenRepository.deleteExpired(Instant.now().minus(7, ChronoUnit.DAYS));
    }

    private SessionTokens createSession(User user, String deviceSummary) {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(properties.getJwt().getRefreshExpirationDays(), ChronoUnit.DAYS);
        refreshTokenRepository.save(new RefreshToken(user, hash(rawRefresh), expiresAt, safeDevice(deviceSummary)));
        String access = jwtService.createAccessToken(user);
        ApiDtos.TokenResponse response = new ApiDtos.TokenResponse(access, "Bearer", jwtService.accessExpirationSeconds(), DtoMapper.user(user));
        return new SessionTokens(response, rawRefresh, expiresAt);
    }

    private String normalizeIdentifier(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("@") && !normalized.startsWith("@")
                ? DomainNormalizer.email(normalized) : DomainNormalizer.username(normalized);
    }

    private String generateAccountNumber(UUID id) {
        long positive = id.getLeastSignificantBits() & Long.MAX_VALUE;
        return String.format("%010d", positive % 10_000_000_000L);
    }

    private String generateDigit(UUID id) {
        return Integer.toString(Math.floorMod(id.hashCode(), 10));
    }

    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.UNNECESSARY); }

    private String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private String safeDevice(String value) {
        if (value == null || value.isBlank()) return "Dispositivo não identificado";
        String clean = value.replaceAll("[\\r\\n]", " ");
        return clean.substring(0, Math.min(clean.length(), 160));
    }

    public record SessionTokens(ApiDtos.TokenResponse response, String refreshToken, Instant refreshExpiresAt) {
    }
}
