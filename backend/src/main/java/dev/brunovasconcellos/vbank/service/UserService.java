package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiDtos;
import dev.brunovasconcellos.vbank.api.ApiException;
import dev.brunovasconcellos.vbank.domain.Account;
import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.domain.RefreshToken;
import dev.brunovasconcellos.vbank.domain.User;
import dev.brunovasconcellos.vbank.repository.AccountRepository;
import dev.brunovasconcellos.vbank.repository.RefreshTokenRepository;
import dev.brunovasconcellos.vbank.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, AccountRepository accountRepository,
                       RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ApiDtos.UserResponse get(UUID userId) { return DtoMapper.user(requireUser(userId)); }

    @Transactional
    public ApiDtos.UserResponse update(UUID userId, String fullName) {
        User user = requireUser(userId);
        user.setFullName(fullName.trim());
        auditService.record(user, "PROFILE_CHANGED", "SUCCESS", "USER", userId.toString(), null);
        return DtoMapper.user(user);
    }

    @Transactional
    public void changePassword(UUID userId, ApiDtos.ChangePasswordRequest request) {
        User user = userRepository.findLockedById(userId).orElseThrow();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "A senha atual está incorreta.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAll(userId, Instant.now());
        auditService.record(user, "PASSWORD_CHANGED", "SUCCESS", "USER", userId.toString(), "todas as sessões revogadas");
    }

    @Transactional(readOnly = true)
    public List<SessionInfo> sessions(UUID userId) {
        return refreshTokenRepository.findByUserId(userId).stream()
                .filter(RefreshToken::isUsable)
                .map(token -> new SessionInfo(token.getId(), token.getDeviceSummary(), token.getCreatedAt(), token.getExpiresAt()))
                .toList();
    }

    @Transactional
    public void selfBlock(UUID userId) {
        User user = userRepository.findLockedById(userId).orElseThrow();
        Account account = accountRepository.findByUserId(userId).orElseThrow();
        user.setStatus(Enums.UserStatus.BLOCKED);
        account.setStatus(Enums.AccountStatus.TEMPORARILY_BLOCKED);
        refreshTokenRepository.revokeAll(userId, Instant.now());
        auditService.record(user, "SELF_BLOCK", "SUCCESS", "ACCOUNT", account.getId().toString(), null);
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VALIDATION_ERROR", "Usuário não encontrado."));
    }

    public record SessionInfo(UUID id, String deviceSummary, Instant createdAt, Instant expiresAt) {
    }
}
