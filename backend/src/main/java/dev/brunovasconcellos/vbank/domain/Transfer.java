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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Entity
@Table(name = "bank_transfer")
public class Transfer {
    @Id
    private UUID id;
    @Column(name = "public_id", nullable = false, unique = true, length = 32)
    private String publicId;
    @Column(name = "end_to_end_id", nullable = false, unique = true, length = 64)
    private String endToEndId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private Account sourceAccount;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_account_id", nullable = false)
    private Account destinationAccount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(length = 140)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Enums.TransferStatus status;
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;
    @Column(name = "key_used", nullable = false, length = 254)
    private String keyUsed;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    protected Transfer() {
    }

    public Transfer(Account source, Account destination, BigDecimal amount, String description,
                    String idempotencyKey, String keyUsed) {
        this.id = UUID.randomUUID();
        this.publicId = "TRX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        String date = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC).format(Instant.now());
        this.endToEndId = "E2E-SANDBOX-" + date + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.sourceAccount = source;
        this.destinationAccount = destination;
        this.amount = amount;
        this.description = description;
        this.status = Enums.TransferStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
        this.keyUsed = keyUsed;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public void complete() { status = Enums.TransferStatus.COMPLETED; completedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getPublicId() { return publicId; }
    public String getEndToEndId() { return endToEndId; }
    public Account getSourceAccount() { return sourceAccount; }
    public Account getDestinationAccount() { return destinationAccount; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public Enums.TransferStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getKeyUsed() { return keyUsed; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getFailureReason() { return failureReason; }
}
