package com.codecore.audit.domain.model.auditentry;

import com.codecore.audit.domain.valueobject.ActionCode;
import com.codecore.audit.domain.valueobject.AuditEntryId;
import com.codecore.audit.domain.valueobject.AuditOutcome;
import com.codecore.audit.domain.valueobject.MembershipId;
import com.codecore.audit.domain.valueobject.ResourceId;
import com.codecore.audit.domain.valueobject.ResourceType;
import com.codecore.audit.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * AuditEntry aggregate root — the immutable record that a significant action occurred within a
 * Tenant (ADR-020).
 * <p>
 * <strong>One sentence:</strong> the immutable record that a significant action occurred within
 * a Tenant.
 * <p>
 * Intentionally small / append-only: no update, void, delete, payload, or metrics. Never embeds
 * PII dumps, SIEM alerts, or Domain Event bus concerns.
 */
public final class AuditEntry {

    private final AuditEntryId id;
    private final TenantId tenantId;
    private final Instant occurredAt;
    private final ActionCode actionCode;
    private final MembershipId actorMembershipId;
    private final ResourceType resourceType;
    private final ResourceId resourceId;
    private final AuditOutcome outcome;
    private final Instant createdAt;

    private AuditEntry(
            AuditEntryId id,
            TenantId tenantId,
            Instant occurredAt,
            ActionCode actionCode,
            MembershipId actorMembershipId,
            ResourceType resourceType,
            ResourceId resourceId,
            AuditOutcome outcome,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.actionCode = Objects.requireNonNull(actionCode, "actionCode");
        this.actorMembershipId = actorMembershipId;
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /**
     * Appends (creates) an immutable audit entry. {@code outcome} defaults to {@link AuditOutcome#SUCCESS}
     * when null. {@code createdAt} is set to {@code occurredAt}.
     */
    public static AuditEntry append(
            AuditEntryId id,
            TenantId tenantId,
            Instant occurredAt,
            ActionCode actionCode,
            MembershipId actorMembershipIdOrNull,
            ResourceType resourceType,
            ResourceId resourceId,
            AuditOutcome outcomeOrNull
    ) {
        Objects.requireNonNull(occurredAt, "occurredAt");
        AuditOutcome outcome = outcomeOrNull == null ? AuditOutcome.SUCCESS : outcomeOrNull;
        return new AuditEntry(
                id,
                tenantId,
                occurredAt,
                actionCode,
                actorMembershipIdOrNull,
                resourceType,
                resourceId,
                outcome,
                occurredAt
        );
    }

    /** Alias of {@link #append} — same append-only factory behavior. */
    public static AuditEntry create(
            AuditEntryId id,
            TenantId tenantId,
            Instant occurredAt,
            ActionCode actionCode,
            MembershipId actorMembershipIdOrNull,
            ResourceType resourceType,
            ResourceId resourceId,
            AuditOutcome outcomeOrNull
    ) {
        return append(
                id,
                tenantId,
                occurredAt,
                actionCode,
                actorMembershipIdOrNull,
                resourceType,
                resourceId,
                outcomeOrNull
        );
    }

    public static AuditEntry reconstitute(
            AuditEntryId id,
            TenantId tenantId,
            Instant occurredAt,
            ActionCode actionCode,
            MembershipId actorMembershipIdOrNull,
            ResourceType resourceType,
            ResourceId resourceId,
            AuditOutcome outcome,
            Instant createdAt
    ) {
        return new AuditEntry(
                id,
                tenantId,
                occurredAt,
                actionCode,
                actorMembershipIdOrNull,
                resourceType,
                resourceId,
                outcome,
                createdAt
        );
    }

    public AuditEntryId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public ActionCode actionCode() {
        return actionCode;
    }

    public Optional<MembershipId> actorMembershipId() {
        return Optional.ofNullable(actorMembershipId);
    }

    public ResourceType resourceType() {
        return resourceType;
    }

    public ResourceId resourceId() {
        return resourceId;
    }

    public AuditOutcome outcome() {
        return outcome;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
