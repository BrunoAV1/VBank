package dev.brunovasconcellos.vbank.service;

import dev.brunovasconcellos.vbank.api.ApiDtos;
import dev.brunovasconcellos.vbank.domain.Account;
import dev.brunovasconcellos.vbank.domain.AuditLog;
import dev.brunovasconcellos.vbank.domain.LedgerEntry;
import dev.brunovasconcellos.vbank.domain.Notification;
import dev.brunovasconcellos.vbank.domain.PixKey;
import dev.brunovasconcellos.vbank.domain.Transfer;
import dev.brunovasconcellos.vbank.domain.User;

public final class DtoMapper {
    private DtoMapper() {
    }

    public static ApiDtos.UserResponse user(User user) {
        return new ApiDtos.UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getUsername(),
                user.getStatus(), user.getRoles(), user.getPinHash() != null, user.getCreatedAt());
    }

    public static ApiDtos.AccountResponse account(Account account) {
        return new ApiDtos.AccountResponse(account.getId(), account.getAgency(), account.getAccountNumber(),
                account.getAccountDigit(), account.getBalance(), account.getDailyLimit(), account.getTransferredToday(),
                account.getStatus(), account.getCreatedAt());
    }

    public static ApiDtos.PixKeyResponse pix(PixKey key) {
        return new ApiDtos.PixKeyResponse(key.getId(), key.getType(), key.getDisplayValue(), key.getStatus(), key.getCreatedAt());
    }

    public static ApiDtos.TransferResponse transfer(Transfer transfer) {
        return new ApiDtos.TransferResponse(transfer.getId(), transfer.getPublicId(), transfer.getEndToEndId(),
                transfer.getAmount(), transfer.getDescription(), transfer.getStatus(),
                transfer.getSourceAccount().getUser().getFullName(), transfer.getDestinationAccount().getUser().getFullName(),
                transfer.getKeyUsed(), transfer.getCreatedAt(), transfer.getCompletedAt(), true);
    }

    public static ApiDtos.LedgerEntryResponse ledger(LedgerEntry entry) {
        return new ApiDtos.LedgerEntryResponse(entry.getId(), entry.getType(), entry.getCategory(), entry.getAmount(),
                entry.getResultingBalance(), entry.getDescription(),
                entry.getTransfer() == null ? null : entry.getTransfer().getId(), entry.getCreatedAt());
    }

    public static ApiDtos.NotificationResponse notification(Notification notification) {
        return new ApiDtos.NotificationResponse(notification.getId(), notification.getTitle(), notification.getMessage(),
                notification.getType(), notification.isRead(), notification.getCreatedAt());
    }

    public static ApiDtos.AuditResponse audit(AuditLog log) {
        return new ApiDtos.AuditResponse(log.getId(), log.getUser() == null ? null : log.getUser().getId(),
                log.getAction(), log.getOutcome(), log.getActorLabel(), log.getTargetType(), log.getTargetId(),
                log.getMetadata(), log.getCreatedAt());
    }

    public static String maskName(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return mask(parts[0]);
        return mask(parts[0]) + " " + mask(parts[parts.length - 1]);
    }

    private static String mask(String part) {
        if (part.length() <= 2) return part.charAt(0) + "*";
        return part.charAt(0) + "*".repeat(Math.min(5, part.length() - 2)) + part.charAt(part.length() - 1);
    }
}

