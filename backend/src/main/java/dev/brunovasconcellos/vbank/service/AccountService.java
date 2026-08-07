package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiDtos;
import dev.brunovasconcellos.vbank.api.ApiException;
import dev.brunovasconcellos.vbank.domain.Account;
import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.repository.AccountRepository;
import dev.brunovasconcellos.vbank.repository.LedgerEntryRepository;
import dev.brunovasconcellos.vbank.repository.NotificationRepository;
import dev.brunovasconcellos.vbank.repository.PixKeyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final PixKeyRepository pixKeyRepository;
    private final NotificationRepository notificationRepository;

    public AccountService(AccountRepository accountRepository, LedgerEntryRepository ledgerRepository,
                          PixKeyRepository pixKeyRepository, NotificationRepository notificationRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.pixKeyRepository = pixKeyRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public ApiDtos.AccountResponse get(UUID userId) { return DtoMapper.account(requireAccount(userId)); }

    @Transactional(readOnly = true)
    public ApiDtos.BalanceResponse balance(UUID userId) {
        Account account = requireAccount(userId);
        return new ApiDtos.BalanceResponse(account.getBalance(), "BRL", true, Instant.now());
    }

    @Transactional(readOnly = true)
    public Page<ApiDtos.LedgerEntryResponse> statement(UUID userId, Enums.LedgerType type,
                                                       Instant from, Instant to, BigDecimal minAmount,
                                                       BigDecimal maxAmount, String search, Pageable pageable) {
        Account account = requireAccount(userId);
        return ledgerRepository.search(account.getId(), type, from, to, minAmount, maxAmount,
                normalizedSearch(search), pageable).map(DtoMapper::ledger);
    }

    @Transactional(readOnly = true)
    public ApiDtos.DashboardResponse dashboard(UUID userId) {
        Account account = requireAccount(userId);
        var recent = ledgerRepository.search(account.getId(), null, null, null, null, null, "",
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))).map(DtoMapper::ledger).getContent();
        var keys = pixKeyRepository.findByAccountAndStatus(account.getId(), Enums.PixKeyStatus.ACTIVE).stream().map(DtoMapper::pix).toList();
        long unread = notificationRepository.findByUserId(userId, Pageable.unpaged()).stream().filter(item -> !item.isRead()).count();
        return new ApiDtos.DashboardResponse(DtoMapper.account(account), recent, keys, unread);
    }

    private Account requireAccount(UUID userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VALIDATION_ERROR", "Conta não encontrada."));
    }

    private String normalizedSearch(String value) { return value == null || value.isBlank() ? "" : value.trim(); }
}
