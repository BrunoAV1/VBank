package dev.brunovasconcellos.vbank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entry")
public class LedgerEntry {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id")
    private Transfer transfer;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Enums.LedgerType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Enums.LedgerCategory category;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(name = "resulting_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal resultingBalance;
    @Column(nullable = false, length = 255)
    private String description;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    public LedgerEntry(Account account, Transfer transfer, Enums.LedgerType type,
                       Enums.LedgerCategory category, BigDecimal amount,
                       BigDecimal resultingBalance, String description) {
        this.id = UUID.randomUUID();
        this.account = account;
        this.transfer = transfer;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.resultingBalance = resultingBalance;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public Transfer getTransfer() { return transfer; }
    public Enums.LedgerType getType() { return type; }
    public Enums.LedgerCategory getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getResultingBalance() { return resultingBalance; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}

