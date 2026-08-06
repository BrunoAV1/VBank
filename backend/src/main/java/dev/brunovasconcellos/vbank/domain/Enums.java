package dev.brunovasconcellos.vbank.domain;

public final class Enums {
    private Enums() {
    }

    public enum Role { USER, ADMIN, SYSTEM }
    public enum UserStatus { ACTIVE, BLOCKED, CLOSED }
    public enum AccountStatus { ACTIVE, TEMPORARILY_BLOCKED, CLOSED, SYSTEM }
    public enum PixKeyType { EMAIL, PHONE, USERNAME, RANDOM }
    public enum PixKeyStatus { ACTIVE, DELETED }
    public enum TransferStatus { PENDING, COMPLETED, FAILED, REVERSED }
    public enum LedgerType { CREDIT, DEBIT }
    public enum LedgerCategory { OPENING_BALANCE, PIX_TRANSFER, SANDBOX_FUNDING, ADMIN_ADJUSTMENT, REVERSAL }
}

