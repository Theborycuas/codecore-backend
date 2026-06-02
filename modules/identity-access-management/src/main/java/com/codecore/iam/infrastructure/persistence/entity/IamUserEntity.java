package com.codecore.iam.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC row mapping for {@code iam.iam_user}. Schema declared per entity (multi-module safe).
 * <p>
 * Lifecycle master column: {@code status}. Column {@code email_verified} is a persisted projection only
 * (see {@link com.codecore.iam.infrastructure.persistence.EmailVerifiedProjection}) — do not call
 * {@link #getEmailVerifiedProjection()} for business logic.
 */
@Table(name = "iam_user", schema = "iam")
public class IamUserEntity implements Persistable<UUID> {

    @Transient
    private boolean newEntity;

    @Id
    private UUID id;

    @Column("tenant_id")
    private UUID tenantId;

    private String email;

    @Column("normalized_email")
    private String normalizedEmail;

    @Column("password_hash")
    private String passwordHash;

    private String status;

    /**
     * Persisted projection of {@link #status} — not master data. Written only via
     * {@link com.codecore.iam.infrastructure.persistence.EmailVerifiedProjection}.
     */
    @Column("email_verified")
    private boolean emailVerifiedProjection;

    @Column("last_login_at")
    private Instant lastLoginAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    public IamUserEntity() {
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setNewEntity(boolean newEntity) {
        this.newEntity = newEntity;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNormalizedEmail() {
        return normalizedEmail;
    }

    public void setNormalizedEmail(String normalizedEmail) {
        this.normalizedEmail = normalizedEmail;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * R2DBC materialization only. Master data is {@link #status} / domain {@code IdentityStatus}.
     */
    public boolean getEmailVerifiedProjection() {
        return emailVerifiedProjection;
    }

    /**
     * Write path only — value must come from {@link com.codecore.iam.infrastructure.persistence.EmailVerifiedProjection}.
     */
    public void setEmailVerifiedProjection(boolean emailVerifiedProjection) {
        this.emailVerifiedProjection = emailVerifiedProjection;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
