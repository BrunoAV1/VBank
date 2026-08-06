package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiException;
import dev.brunovasconcellos.vbank.domain.User;
import dev.brunovasconcellos.vbank.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class PinService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final RateLimitService rateLimitService;

    public PinService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      AuditService auditService, RateLimitService rateLimitService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.rateLimitService = rateLimitService;
    }

    @Transactional
    public void create(UUID userId, String pin) {
        User user = requireLocked(userId);
        if (user.getPinHash() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "VALIDATION_ERROR", "O PIN já foi criado. Use a alteração de PIN.");
        }
        user.setPinHash(passwordEncoder.encode(pin));
        auditService.record(user, "PIN_CREATED", "SUCCESS", "USER", userId.toString(), null);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public void change(UUID userId, String currentPin, String newPin) {
        User user = requireLocked(userId);
        verifyStateAndMatch(user, currentPin);
        user.setPinHash(passwordEncoder.encode(newPin));
        user.setPinFailedAttempts(0);
        user.setPinBlockedUntil(null);
        auditService.record(user, "PIN_CHANGED", "SUCCESS", "USER", userId.toString(), null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)
    public void verifyForTransfer(UUID userId, String pin) {
        rateLimitService.consume("pin", userId.toString(), 12, Duration.ofMinutes(15));
        User user = requireLocked(userId);
        verifyStateAndMatch(user, pin);
        if (user.getPinFailedAttempts() != 0 || user.getPinBlockedUntil() != null) {
            user.setPinFailedAttempts(0);
            user.setPinBlockedUntil(null);
        }
    }

    private void verifyStateAndMatch(User user, String pin) {
        if (user.getPinHash() == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PIN", "Crie um PIN de 6 dígitos antes de transferir.");
        }
        if (user.getPinBlockedUntil() != null && user.getPinBlockedUntil().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.LOCKED, "PIN_TEMPORARILY_BLOCKED", "O PIN está bloqueado temporariamente após cinco tentativas incorretas.");
        }
        if (!passwordEncoder.matches(pin, user.getPinHash())) {
            int failures = user.getPinFailedAttempts() + 1;
            user.setPinFailedAttempts(failures);
            if (failures >= 5) {
                user.setPinFailedAttempts(5);
                user.setPinBlockedUntil(Instant.now().plus(Duration.ofMinutes(15)));
                auditService.record(user, "PIN_BLOCKED", "DENIED", "USER", user.getId().toString(), "cinco tentativas incorretas");
                throw new ApiException(HttpStatus.LOCKED, "PIN_TEMPORARILY_BLOCKED", "O PIN foi bloqueado por 15 minutos.");
            }
            auditService.record(user, "PIN_FAILED", "DENIED", "USER", user.getId().toString(), "tentativa " + failures + " de 5");
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PIN", "PIN incorreto. Restam " + (5 - failures) + " tentativas.");
        }
    }

    private User requireLocked(UUID userId) {
        return userRepository.findLockedById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VALIDATION_ERROR", "Usuário não encontrado."));
    }
}
