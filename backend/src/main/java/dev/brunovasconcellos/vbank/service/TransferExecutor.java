package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiDtos;
import dev.brunovasconcellos.vbank.api.ApiException;
import dev.brunovasconcellos.vbank.domain.Account;
import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.domain.LedgerEntry;
import dev.brunovasconcellos.vbank.domain.Notification;
import dev.brunovasconcellos.vbank.domain.PixKey;
import dev.brunovasconcellos.vbank.domain.Transfer;
import dev.brunovasconcellos.vbank.repository.AccountRepository;
import dev.brunovasconcellos.vbank.repository.LedgerEntryRepository;
import dev.brunovasconcellos.vbank.repository.NotificationRepository;
import dev.brunovasconcellos.vbank.repository.PixKeyRepository;
import dev.brunovasconcellos.vbank.repository.TransferRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransferExecutor {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final AccountRepository accountRepository;
    private final PixKeyRepository pixKeyRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final NotificationRepository notificationRepository;
    private final AuditService auditService;

    public TransferExecutor(AccountRepository accountRepository, PixKeyRepository pixKeyRepository,
                            TransferRepository transferRepository, LedgerEntryRepository ledgerRepository,
                            NotificationRepository notificationRepository, AuditService auditService) {
        this.accountRepository = accountRepository;
        this.pixKeyRepository = pixKeyRepository;
        this.transferRepository = transferRepository;
        this.ledgerRepository = ledgerRepository;
        this.notificationRepository = notificationRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ApiDtos.TransferResponse findExisting(UUID userId, String idempotencyKey, ApiDtos.TransferRequest request) {
        Account source = accountRepository.findByUserId(userId).orElseThrow();
        return transferRepository.findBySourceAccountIdAndIdempotencyKey(source.getId(), idempotencyKey)
                .map(transfer -> validatedReplay(transfer, request)).orElse(null);
    }

    @Transactional
    public ApiDtos.TransferResponse execute(UUID userId, ApiDtos.TransferRequest request, String idempotencyKey) {
        Account sourceSnapshot = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VALIDATION_ERROR", "Conta de demonstração não encontrada."));
        ApiDtos.TransferResponse replay = transferRepository
                .findBySourceAccountIdAndIdempotencyKey(sourceSnapshot.getId(), idempotencyKey)
                .map(transfer -> validatedReplay(transfer, request)).orElse(null);
        if (replay != null) return replay;

        String normalizedKey = DomainNormalizer.resolvePixKey(request.key());
        PixKey resolved = pixKeyRepository.findByNormalizedValueAndStatus(normalizedKey, Enums.PixKeyStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PIX_KEY_NOT_FOUND", "Chave interna não encontrada."));
        UUID destinationId = resolved.getAccount().getId();
        if (sourceSnapshot.getId().equals(destinationId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SELF_TRANSFER_NOT_ALLOWED", "Não é possível transferir para a própria conta.");
        }

        List<UUID> orderedIds = List.of(sourceSnapshot.getId(), destinationId).stream().sorted(Comparator.naturalOrder()).toList();
        Map<UUID, Account> locked = accountRepository.findAllLockedById(orderedIds).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        Account source = requireLocked(locked, sourceSnapshot.getId());
        Account destination = requireLocked(locked, destinationId);

        ApiDtos.TransferResponse concurrentReplay = transferRepository
                .findBySourceAccountIdAndIdempotencyKey(source.getId(), idempotencyKey)
                .map(transfer -> validatedReplay(transfer, request)).orElse(null);
        if (concurrentReplay != null) return concurrentReplay;

        PixKey rechecked = pixKeyRepository.findByNormalizedValueAndStatus(normalizedKey, Enums.PixKeyStatus.ACTIVE)
                .filter(key -> key.getAccount().getId().equals(destination.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PIX_KEY_NOT_FOUND", "A chave interna deixou de estar disponível."));
        validateAccounts(source, destination);

        BigDecimal amount;
        try {
            amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Use no máximo duas casas decimais.");
        }
        if (amount.compareTo(new BigDecimal("0.01")) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "O valor mínimo é R$ 0,01.");
        }
        resetDailyLimitIfNeeded(source);
        if (source.getBalance().compareTo(amount) < 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE", "O saldo fictício disponível não é suficiente para esta transferência.");
        }
        if (source.getTransferredToday().add(amount).compareTo(source.getDailyLimit()) > 0) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DAILY_LIMIT_EXCEEDED", "A transferência excede o limite diário da conta de demonstração.");
        }

        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));
        source.setTransferredToday(source.getTransferredToday().add(amount));

        Transfer transfer = new Transfer(source, destination, amount, cleanDescription(request.description()),
                idempotencyKey, rechecked.getDisplayValue());
        transfer.complete();
        transferRepository.save(transfer);
        accountRepository.saveAll(List.of(source, destination));
        ledgerRepository.save(new LedgerEntry(source, transfer, Enums.LedgerType.DEBIT, Enums.LedgerCategory.PIX_TRANSFER,
                amount, source.getBalance(), "Transferência sandbox para " + DtoMapper.maskName(destination.getUser().getFullName())));
        ledgerRepository.save(new LedgerEntry(destination, transfer, Enums.LedgerType.CREDIT, Enums.LedgerCategory.PIX_TRANSFER,
                amount, destination.getBalance(), "Transferência sandbox recebida de " + DtoMapper.maskName(source.getUser().getFullName())));
        notificationRepository.save(new Notification(source.getUser(), "Transferência sandbox concluída",
                "Você enviou " + formatMoney(amount) + " em valor fictício.", "TRANSFER_SENT"));
        notificationRepository.save(new Notification(destination.getUser(), "Transferência sandbox recebida",
                "Você recebeu " + formatMoney(amount) + " em valor fictício.", "TRANSFER_RECEIVED"));
        auditService.record(source.getUser(), "TRANSFER_COMPLETED", "SUCCESS", "TRANSFER", transfer.getId().toString(),
                "valor=" + amount + "; destinatário=" + destination.getId());
        return DtoMapper.transfer(transfer);
    }

    @Transactional(readOnly = true)
    public ApiDtos.TransferResponse getForUser(UUID userId, UUID transferId) {
        Account account = accountRepository.findByUserId(userId).orElseThrow();
        Transfer transfer = transferRepository.findDetailedById(transferId)
                .filter(item -> item.getSourceAccount().getId().equals(account.getId())
                        || item.getDestinationAccount().getId().equals(account.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VALIDATION_ERROR", "Transferência não encontrada."));
        return DtoMapper.transfer(transfer);
    }

    @Transactional(readOnly = true)
    public Transfer getEntityForUser(UUID userId, UUID transferId) {
        Account account = accountRepository.findByUserId(userId).orElseThrow();
        return transferRepository.findDetailedById(transferId)
                .filter(item -> item.getSourceAccount().getId().equals(account.getId())
                        || item.getDestinationAccount().getId().equals(account.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VALIDATION_ERROR", "Transferência não encontrada."));
    }

    @Transactional(readOnly = true)
    public Page<ApiDtos.TransferResponse> list(UUID userId, Pageable pageable) {
        Account account = accountRepository.findByUserId(userId).orElseThrow();
        return transferRepository.findForAccount(account.getId(), pageable).map(DtoMapper::transfer);
    }

    private Account requireLocked(Map<UUID, Account> locked, UUID id) {
        Account account = locked.get(id);
        if (account == null) throw new ApiException(HttpStatus.NOT_FOUND, "VALIDATION_ERROR", "Conta não encontrada durante a transferência.");
        return account;
    }

    private void validateAccounts(Account source, Account destination) {
        if (source.getStatus() != Enums.AccountStatus.ACTIVE || source.getUser().getStatus() != Enums.UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_BLOCKED", "A conta de origem está bloqueada ou encerrada.");
        }
        if (destination.getStatus() != Enums.AccountStatus.ACTIVE || destination.getUser().getStatus() != Enums.UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ACCOUNT_BLOCKED", "A conta de destino não pode receber transferências.");
        }
    }

    private void resetDailyLimitIfNeeded(Account account) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (!today.equals(account.getLimitReferenceDate())) {
            account.setLimitReferenceDate(today);
            account.setTransferredToday(BigDecimal.ZERO.setScale(2));
        }
    }

    private String cleanDescription(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private ApiDtos.TransferResponse validatedReplay(Transfer transfer, ApiDtos.TransferRequest request) {
        boolean sameAmount = transfer.getAmount().compareTo(request.amount()) == 0;
        boolean sameKey = DomainNormalizer.resolvePixKey(transfer.getKeyUsed())
                .equals(DomainNormalizer.resolvePixKey(request.key()));
        boolean sameDescription = Objects.equals(transfer.getDescription(), cleanDescription(request.description()));
        if (!sameAmount || !sameKey || !sameDescription) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_IDEMPOTENCY_KEY",
                    "Esta Idempotency-Key já foi usada com dados diferentes.");
        }
        return DtoMapper.transfer(transfer);
    }

    private String formatMoney(BigDecimal amount) { return "R$ " + amount.toPlainString().replace('.', ','); }
}
