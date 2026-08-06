package dev.brunovasconcellos.vbank.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class User {
    @Id
    private UUID id;
    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(nullable = false, unique = true, length = 40)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(name = "pin_hash", length = 100)
    private String pinHash;
    @Column(name = "pin_failed_attempts", nullable = false)
    private int pinFailedAttempts;
    @Column(name = "pin_blocked_until")
    private Instant pinBlockedUntil;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.UserStatus status;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 12)
    private Set<Enums.Role> roles = new HashSet<>();
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    public User(String fullName, String email, String username, String passwordHash, Set<Enums.Role> roles) {
        this.id = UUID.randomUUID();
        this.fullName = fullName;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = new HashSet<>(roles);
        this.status = Enums.UserStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public int getPinFailedAttempts() { return pinFailedAttempts; }
    public void setPinFailedAttempts(int pinFailedAttempts) { this.pinFailedAttempts = pinFailedAttempts; }
    public Instant getPinBlockedUntil() { return pinBlockedUntil; }
    public void setPinBlockedUntil(Instant pinBlockedUntil) { this.pinBlockedUntil = pinBlockedUntil; }
    public Enums.UserStatus getStatus() { return status; }
    public void setStatus(Enums.UserStatus status) { this.status = status; }
    public Set<Enums.Role> getRoles() { return Set.copyOf(roles); }
    public void addRole(Enums.Role role) { roles.add(role); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

