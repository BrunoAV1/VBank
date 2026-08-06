package dev.brunovasconcellos.vbank.api;

import dev.brunovasconcellos.vbank.domain.Enums;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ApiDtos {
    private ApiDtos() {
    }

    public static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$";
    public static final String PIN_PATTERN = "^\\d{6}$";

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 160) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,40}$") String username,
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = "use 8 a 72 caracteres, maiúscula, minúscula e número") String password,
            @NotBlank String passwordConfirmation,
            @AssertTrue(message = "aceite os termos do ambiente fictício") boolean acceptedTerms) {
    }

    public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {
    }

    public record UserResponse(UUID id, String fullName, String email, String username,
                               Enums.UserStatus status, Set<Enums.Role> roles, boolean pinConfigured,
                               Instant createdAt) {
    }

    public record UpdateProfileRequest(@NotBlank @Size(min = 3, max = 160) String fullName) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Pattern(regexp = PASSWORD_PATTERN) String newPassword) {
    }

    public record CreatePinRequest(@NotBlank @Pattern(regexp = PIN_PATTERN) String pin) {
    }

    public record ChangePinRequest(@NotBlank @Pattern(regexp = PIN_PATTERN) String currentPin,
                                   @NotBlank @Pattern(regexp = PIN_PATTERN) String newPin) {
    }

    public record AccountResponse(UUID id, String agency, String accountNumber, String accountDigit,
                                  BigDecimal balance, BigDecimal dailyLimit, BigDecimal transferredToday,
                                  Enums.AccountStatus status, Instant createdAt) {
    }

    public record BalanceResponse(BigDecimal balance, String currency, boolean fictitious, Instant asOf) {
    }

    public record PixKeyRequest(@NotNull Enums.PixKeyType type, @Size(max = 254) String value) {
    }

    public record PixKeyResponse(UUID id, Enums.PixKeyType type, String displayValue,
                                 Enums.PixKeyStatus status, Instant createdAt) {
    }

    public record ResolvePixKeyRequest(@NotBlank @Size(max = 254) String key) {
    }

    public record ResolvedPixKeyResponse(String maskedName, Enums.PixKeyType type,
                                         String keyDisplay, String accountStatus, boolean internalOnly) {
    }

    public record TransferRequest(
            @NotBlank @Size(max = 254) String key,
            @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @Size(max = 140) String description,
            @NotBlank @Pattern(regexp = PIN_PATTERN) String pin) {
    }

    public record TransferResponse(UUID id, String publicId, String endToEndId, BigDecimal amount,
                                   String description, Enums.TransferStatus status, String payerName,
                                   String recipientName, String keyUsed, Instant createdAt, Instant completedAt,
                                   boolean fictitious) {
    }

    public record LedgerEntryResponse(UUID id, Enums.LedgerType type, Enums.LedgerCategory category,
                                      BigDecimal amount, BigDecimal resultingBalance, String description,
                                      UUID transferId, Instant createdAt) {
    }

    public record FundingStatusResponse(boolean available, BigDecimal currentBalance,
                                        BigDecimal amountAvailable, Instant nextAvailableAt) {
    }

    public record FundingResponse(BigDecimal amount, BigDecimal balance, Instant fundedAt, boolean fictitious) {
    }

    public record NotificationResponse(UUID id, String title, String message, String type,
                                       boolean read, Instant createdAt) {
    }

    public record HealthResponse(String status, String application, String database,
                                 String version, Instant timestamp) {
    }

    public record AdminAdjustmentRequest(
            @NotNull @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @NotBlank @Size(max = 140) String reason) {
    }

    public record AdminUserResponse(UserResponse user, AccountResponse account) {
    }

    public record AuditResponse(UUID id, UUID userId, String action, String outcome,
                                String actorLabel, String targetType, String targetId,
                                String metadata, Instant createdAt) {
    }

    public record SessionResponse(String message) {
    }

    public record DashboardResponse(AccountResponse account, List<LedgerEntryResponse> recentEntries,
                                    List<PixKeyResponse> keys, long unreadNotifications) {
    }
}

