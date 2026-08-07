package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiDtos;
import dev.brunovasconcellos.vbank.api.ApiException;
import dev.brunovasconcellos.vbank.domain.Account;
import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.domain.LedgerEntry;
import dev.brunovasconcellos.vbank.domain.Notification;
import dev.brunovasconcellos.vbank.domain.Transfer;
import dev.brunovasconcellos.vbank.domain.User;
import dev.brunovasconcellos.vbank.repository.AccountRepository;
import dev.brunovasconcellos.vbank.repository.AuditLogRepository;
import dev.brunovasconcellos.vbank.repository.LedgerEntryRepository;
import dev.brunovasconcellos.vbank.repository.NotificationRepository;
import dev.brunovasconcellos.vbank.repository.RefreshTokenRepository;
import dev.brunovasconcellos.vbank.repository.TransferRepository;
import dev.brunovasconcellos.vbank.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final AuditLogRepository auditLogRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final NotificationRepository notificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    public AdminService(UserRepository userRepository, AccountRepository accountRepository,
                        TransferRepository transferRepository, AuditLogRepository auditLogRepository,
                        LedgerEntryRepository ledgerRepository, NotificationRepository notificationRepository,
                        RefreshTokenRepository refreshTokenRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.auditLogRepository = auditLogRepository;
        this.ledgerRepository = ledgerRepository;
        this.notificationRepository = notificationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ApiDtos.AdminUserResponse> users(String search, Pageable pageable) {
        return userRepository.search(blank(search), pageable)
                .map(user -> new ApiDtos.AdminUserResponse(DtoMapper.user(user),
                        accountRepository.findByUserId(user.getId()).map(DtoMapper::account).orElse(null)));
    }

    @Transactional(readOnly = true)
    public ApiDtos.AdminUserResponse user(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VALIDATION_ERROR", "Usuário não encontrado."));
        return new ApiDtos.AdminUserResponse(DtoMapper.user(user), accountRepository.findByUserId(id).map(DtoMapper::account).orElse(null));
    }

    @Transactional
    public ApiDtos.AccountResponse block(UUID adminId, UUID accountId) {
        Account account = accountRepository.findLockedById(accountId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VALIDATION_ERROR", "Conta não encontrada."));
        rejectSystem(account);
        account.setStatus(Enums.AccountStatus.TEMPORARILY_BLOCKED);
        account.getUser().setStatus(Enums.UserStatus.BLOCKED);
        refreshTokenRepository.revokeAll(account.getUser().getId(), Instant.now());
        auditService.record(requireAdmin(adminId), "ADMIN_BLOCK", "SUCCESS", "ACCOUNT", accountId.toString(), null);
        return DtoMapper.account(account);
    }

    @Transactional
    public ApiDtos.AccountResponse unblock(UUID adminId, UUID accountId) {
        Account account = accountRepository.findLockedById(accountId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VALIDATION_ERROR", "Conta não encontrada."));
        rejectSystem(account);
        if (account.getStatus() == Enums.AccountStatus.CLOSED || account.getUser().getStatus() == Enums.UserStatus.CLOSED) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ACCOUNT_BLOCKED", "Uma conta encerrada não pode ser desbloqueada.");
        }
        account.setStatus(Enums.AccountStatus.ACTIVE);
        account.getUser().setStatus(Enums.UserStatus.ACTIVE);
        auditService.record(requireAdmin(adminId), "ADMIN_UNBLOCK", "SUCCESS", "ACCOUNT", accountId.toString(), null);
        return DtoMapper.account(account);
    }

    @Transactional
    public ApiDtos.AccountResponse adjust(UUID adminId, UUID accountId, ApiDtos.AdminAdjustmentRequest request) {
        BigDecimal signed;
        try { signed = request.amount().setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException exception) { throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Use no máximo duas casas decimais."); }
        if (signed.signum() == 0) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "O ajuste não pode ser zero.");
        Account targetSnapshot = accountRepository.findById(accountId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VALIDATION_ERROR", "Conta não encontrada."));
        rejectSystem(targetSnapshot);
        Account systemSnapshot = accountRepository.findByStatus(Enums.AccountStatus.SYSTEM).orElseThrow();
        List<UUID> ids = List.of(targetSnapshot.getId(), systemSnapshot.getId()).stream().sorted(Comparator.naturalOrder()).toList();
        Map<UUID, Account> locked = accountRepository.findAllLockedById(ids).stream().collect(Collectors.toMap(Account::getId, Function.identity()));
        Account target = locked.get(accountId);
        Account system = locked.get(systemSnapshot.getId());
        BigDecimal absolute = signed.abs();
        if (signed.signum() > 0) {
            if (system.getBalance().compareTo(absolute) < 0) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE", "Reserva fictícia interna insuficiente.");
            system.setBalance(system.getBalance().subtract(absolute));
            target.setBalance(target.getBalance().add(absolute));
            ledgerRepository.save(new LedgerEntry(system, null, Enums.LedgerType.DEBIT, Enums.LedgerCategory.ADMIN_ADJUSTMENT, absolute, system.getBalance(), request.reason()));
            ledgerRepository.save(new LedgerEntry(target, null, Enums.LedgerType.CREDIT, Enums.LedgerCategory.ADMIN_ADJUSTMENT, absolute, target.getBalance(), request.reason()));
        } else {
            if (target.getBalance().compareTo(absolute) < 0) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE", "O ajuste deixaria o saldo negativo.");
            target.setBalance(target.getBalance().subtract(absolute));
            system.setBalance(system.getBalance().add(absolute));
            ledgerRepository.save(new LedgerEntry(target, null, Enums.LedgerType.DEBIT, Enums.LedgerCategory.ADMIN_ADJUSTMENT, absolute, target.getBalance(), request.reason()));
            ledgerRepository.save(new LedgerEntry(system, null, Enums.LedgerType.CREDIT, Enums.LedgerCategory.ADMIN_ADJUSTMENT, absolute, system.getBalance(), request.reason()));
        }
        notificationRepository.save(new Notification(target.getUser(), "Ajuste administrativo sandbox",
                "Um ajuste fictício foi registrado: " + request.reason(), "ADMIN_ADJUSTMENT"));
        auditService.record(requireAdmin(adminId), "ADMIN_ADJUSTMENT", "SUCCESS", "ACCOUNT", accountId.toString(), "valor=" + signed + "; motivo=" + request.reason());
        return DtoMapper.account(target);
    }

    @Transactional(readOnly = true)
    public Page<ApiDtos.TransferResponse> transfers(String search, Enums.TransferStatus status,
                                                     BigDecimal minAmount, BigDecimal maxAmount,
                                                     Instant from, Instant to, Pageable pageable) {
        return transferRepository.findAll(transferFilters(blank(search), status, minAmount, maxAmount, from, to), pageable)
                .map(DtoMapper::transfer);
    }

    @Transactional(readOnly = true)
    public Page<ApiDtos.AuditResponse> audits(String search, Pageable pageable) {
        return auditLogRepository.search(blank(search), pageable).map(DtoMapper::audit);
    }

    private User requireAdmin(UUID id) { return userRepository.findById(id).orElseThrow(); }
    private void rejectSystem(Account account) {
        if (account.getStatus() == Enums.AccountStatus.SYSTEM) throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_BLOCKED", "A conta interna do sistema não pode ser alterada por esta API.");
    }
    private String blank(String value) { return value == null || value.isBlank() ? "" : value.trim(); }

    private Specification<Transfer> transferFilters(String search, Enums.TransferStatus status,
                                                     BigDecimal minAmount, BigDecimal maxAmount,
                                                     Instant from, Instant to) {
        return (root, query, criteria) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!search.isEmpty()) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                var sourceUser = root.join("sourceAccount").join("user");
                var destinationUser = root.join("destinationAccount").join("user");
                predicates.add(criteria.or(
                        criteria.like(criteria.lower(root.<String>get("publicId")), pattern),
                        criteria.like(criteria.lower(root.<String>get("endToEndId")), pattern),
                        criteria.like(criteria.lower(sourceUser.<String>get("fullName")), pattern),
                        criteria.like(criteria.lower(destinationUser.<String>get("fullName")), pattern)));
            }
            if (status != null) predicates.add(criteria.equal(root.<Enums.TransferStatus>get("status"), status));
            if (minAmount != null) predicates.add(criteria.greaterThanOrEqualTo(root.<BigDecimal>get("amount"), minAmount));
            if (maxAmount != null) predicates.add(criteria.lessThanOrEqualTo(root.<BigDecimal>get("amount"), maxAmount));
            if (from != null) predicates.add(criteria.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from));
            if (to != null) predicates.add(criteria.lessThan(root.<Instant>get("createdAt"), to));
            return criteria.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
