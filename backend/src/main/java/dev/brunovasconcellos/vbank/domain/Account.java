package dev.brunovasconcellos.vbank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "account")
public class Account {
    @Id
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    @Column(nullable = false, length = 8)
    private String agency;
    @Column(name = "account_number", nullable = false, length = 16)
    private String accountNumber;
    @Column(name = "account_digit", nullable = false, length = 2)
    private String accountDigit;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;
    @Column(name = "daily_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyLimit;
    @Column(name = "transferred_today", nullable = false, precision = 19, scale = 2)
    private BigDecimal transferredToday;
    @Column(name = "limit_reference_date", nullable = false)
    private LocalDate limitReferenceDate;
    @Column(name = "last_sandbox_funding_at")
    private Instant lastSandboxFundingAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Enums.AccountStatus status;
    @Version
    private long version;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Account() {
    }

    public Account(User user, String agency, String accountNumber, String accountDigit, BigDecimal dailyLimit) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.agency = agency;
        this.accountNumber = accountNumber;
        this.accountDigit = accountDigit;
        this.balance = BigDecimal.ZERO.setScale(2);
        this.dailyLimit = dailyLimit;
        this.transferredToday = BigDecimal.ZERO.setScale(2);
        this.limitReferenceDate = LocalDate.now();
        this.status = Enums.AccountStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getAgency() { return agency; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountDigit() { return accountDigit; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
    public BigDecimal getTransferredToday() { return transferredToday; }
    public void setTransferredToday(BigDecimal transferredToday) { this.transferredToday = transferredToday; }
    public LocalDate getLimitReferenceDate() { return limitReferenceDate; }
    public void setLimitReferenceDate(LocalDate limitReferenceDate) { this.limitReferenceDate = limitReferenceDate; }
    public Instant getLastSandboxFundingAt() { return lastSandboxFundingAt; }
    public void setLastSandboxFundingAt(Instant value) { this.lastSandboxFundingAt = value; }
    public Enums.AccountStatus getStatus() { return status; }
    public void setStatus(Enums.AccountStatus status) { this.status = status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

