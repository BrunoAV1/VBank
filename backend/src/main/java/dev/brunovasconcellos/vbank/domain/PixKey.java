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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pix_key")
public class PixKey {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Enums.PixKeyType type;
    @Column(name = "display_value", nullable = false, length = 254)
    private String displayValue;
    @Column(name = "normalized_value", nullable = false, length = 254)
    private String normalizedValue;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Enums.PixKeyStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PixKey() {
    }

    public PixKey(Account account, Enums.PixKeyType type, String displayValue, String normalizedValue) {
        this.id = UUID.randomUUID();
        this.account = account;
        this.type = type;
        this.displayValue = displayValue;
        this.normalizedValue = normalizedValue;
        this.status = Enums.PixKeyStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public Enums.PixKeyType getType() { return type; }
    public String getDisplayValue() { return displayValue; }
    public String getNormalizedValue() { return normalizedValue; }
    public Enums.PixKeyStatus getStatus() { return status; }
    public void delete() { this.status = Enums.PixKeyStatus.DELETED; }
    public Instant getCreatedAt() { return createdAt; }
}

