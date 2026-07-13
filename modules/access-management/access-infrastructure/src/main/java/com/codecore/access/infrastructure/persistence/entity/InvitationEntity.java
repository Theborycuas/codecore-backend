package com.codecore.access.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC row mapping for {@code access.invitation}.
 */
@Table(name = "invitation", schema = "access")
public class InvitationEntity implements Persistable<UUID> {

    @Transient
    private boolean newEntity;

    @Id
    @Column("invitation_id")
    private UUID invitationId;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("invited_email")
    private String invitedEmail;

    @Column("invited_role_code")
    private String invitedRoleCode;

    @Column("invited_by_membership_id")
    private UUID invitedByMembershipId;

    @Column("token_hash")
    private String tokenHash;

    @Column("expires_at")
    private Instant expiresAt;

    private String status;

    @Column("resulting_membership_id")
    private UUID resultingMembershipId;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Column("accepted_at")
    private Instant acceptedAt;

    @Column("revoked_at")
    private Instant revokedAt;

    public InvitationEntity() {
    }

    @Override
    public UUID getId() {
        return invitationId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void setNewEntity(boolean newEntity) {
        this.newEntity = newEntity;
    }

    public UUID getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(UUID invitationId) {
        this.invitationId = invitationId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public void setInvitedEmail(String invitedEmail) {
        this.invitedEmail = invitedEmail;
    }

    public String getInvitedRoleCode() {
        return invitedRoleCode;
    }

    public void setInvitedRoleCode(String invitedRoleCode) {
        this.invitedRoleCode = invitedRoleCode;
    }

    public UUID getInvitedByMembershipId() {
        return invitedByMembershipId;
    }

    public void setInvitedByMembershipId(UUID invitedByMembershipId) {
        this.invitedByMembershipId = invitedByMembershipId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getResultingMembershipId() {
        return resultingMembershipId;
    }

    public void setResultingMembershipId(UUID resultingMembershipId) {
        this.resultingMembershipId = resultingMembershipId;
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

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
