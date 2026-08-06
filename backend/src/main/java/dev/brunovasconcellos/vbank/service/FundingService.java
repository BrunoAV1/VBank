package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiDtos;
import dev.brunovasconcellos.vbank.api.ApiException;
import dev.brunovasconcellos.vbank.domain.Account;
import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.domain.LedgerEntry;
import dev.brunovasconcellos.vbank.domain.Notification;
import dev.brunovasconcellos.vbank.repository.AccountRepository;
import dev.brunovasconcellos.vbank.repository.LedgerEntryRepository;
import dev.brunovasconcellos.vbank.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FundingService {
    private static final BigDecimal TARGET = new BigDecimal("50000.00");
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final NotificationRepository notificationRepository;
    private final AuditService auditService;

    public FundingService(AccountRepository accountRepository, LedgerEntryRepository ledgerRepository,
                          NotificationRepository notificationRepository, AuditService auditService) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.notificationRepository = notificationRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ApiDtos.FundingStatusResponse status(UUID userId) {
        Account account = accountRepository.findByUserId(userId).orElseThrow();
        Instant next = account.getLastSandboxFundingAt() == null ? null : account.getLastSandboxFundingAt().plus(Duration.ofHours(24));
        boolean cooldownDone = next == null || !next.isAfter(Instant.now());
        BigDecimal difference = account.getBalance().compareTo(TARGET) < 0 ? TARGET.subtract(account.getBalance()) : BigDecimal.ZERO.setScale(2);
        return new ApiDtos.FundingStatusResponse(cooldownDone && difference.signum() > 0
                && account.getStatus() == Enums.AccountStatus.ACTIVE, account.getBalance(), difference, next);
    }

    @Transactional
    public ApiDtos.FundingResponse fund(UUID userId) {
        Account targetSnapshot = accountRepository.findByUserId(userId).orElseThrow();
        Account systemSnapshot = accountRepository.findByStatus(Enums.AccountStatus.SYSTEM).orElseThrow();
        List<UUID> ids = List.of(targetSnapshot.getId(), systemSnapshot.getId()).stream().sorted(Comparator.naturalOrder()).toList();
        Map<UUID, Account> locked = accountRepository.findAllLockedById(ids).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        Account target = locked.get(targetSnapshot.getId());
        Account system = locked.get(systemSnapshot.getId());
        if (target.getStatus() != Enums.AccountStatus.ACTIVE || target.getUser().getStatus() != Enums.UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_BLOCKED", "A conta não pode receber recarga sandbox.");
        }
        Instant now = Instant.now();
        if (target.getBalance().compareTo(TARGET) >= 0
                || (target.getLastSandboxFundingAt() != null && target.getLastSandboxFundingAt().plus(Duration.ofHours(24)).isAfter(now))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SANDBOX_FUNDING_NOT_AVAILABLE",
                    "A recarga só está disponível abaixo de R$ 50.000,00 e uma vez a cada 24 horas.");
        }
        BigDecimal amount = TARGET.subtract(target.getBalance());
        if (system.getBalance().compareTo(amount) < 0) throw new IllegalStateException("Reserva fictícia interna insuficiente.");
        system.setBalance(system.getBalance().subtract(amount));
        target.setBalance(TARGET);
        target.setLastSandboxFundingAt(now);
        ledgerRepository.save(new LedgerEntry(system, null, Enums.LedgerType.DEBIT, Enums.LedgerCategory.SANDBOX_FUNDING,
                amount, system.getBalance(), "Recarga sandbox destinada a conta de demonstração"));
        ledgerRepository.save(new LedgerEntry(target, null, Enums.LedgerType.CREDIT, Enums.LedgerCategory.SANDBOX_FUNDING,
                amount, target.getBalance(), "Recarga de saldo fictício"));
        notificationRepository.save(new Notification(target.getUser(), "Recarga sandbox concluída",
                "Seu saldo fictício voltou a R$ 50.000,00.", "SANDBOX_FUNDING"));
        auditService.record(target.getUser(), "SANDBOX_FUNDING", "SUCCESS", "ACCOUNT", target.getId().toString(), "valor=" + amount);
        return new ApiDtos.FundingResponse(amount, target.getBalance(), now, true);
    }
}

