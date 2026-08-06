package dev.brunovasconcellos.vbank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false, length = 48)
    private String action;
    @Column(nullable = false, length = 16)
    private String outcome;
    @Column(name = "actor_label", length = 120)
    private String actorLabel;
    @Column(name = "target_type", length = 48)
    private String targetType;
    @Column(name = "target_id", length = 64)
    private String targetId;
    @Column(length = 500)
    private String metadata;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public AuditLog(User user, String action, String outcome, String actorLabel,
                    String targetType, String targetId, String metadata) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.action = action;
        this.outcome = outcome;
        this.actorLabel = actorLabel;
        this.targetType = targetType;
        this.targetId = targetId;
        this.metadata = metadata;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getAction() { return action; }
    public String getOutcome() { return outcome; }
    public String getActorLabel() { return actorLabel; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}

