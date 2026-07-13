package com.codecore.audit.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC row mapping for {@code audit.audit_entry}.
 */
@Table(name = "audit_entry", schema = "audit")
public class AuditEntryEntity implements Persistable<UUID> {

    @Transient
    private boolean newEntity;

    @Id
    @Column("audit_entry_id")
    private UUID auditEntryId;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("occurred_at")
    private Instant occurredAt;

    @Column("action_code")
    private String actionCode;

    @Column("actor_membership_id")
    private UUID actorMembershipId;

    @Column("resource_type")
    private String resourceType;

    @Column("resource_id")
    private UUID resourceId;

    private String outcome;

    @Column("created_at")
    private Instant createdAt;

    public AuditEntryEntity() {
    }

    @Override
    public UUID getId() {
        return auditEntryId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void setNewEntity(boolean newEntity) {
        this.newEntity = newEntity;
    }

    public UUID getAuditEntryId() {
        return auditEntryId;
    }

    public void setAuditEntryId(UUID auditEntryId) {
        this.auditEntryId = auditEntryId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public UUID getActorMembershipId() {
        return actorMembershipId;
    }

    public void setActorMembershipId(UUID actorMembershipId) {
        this.actorMembershipId = actorMembershipId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
