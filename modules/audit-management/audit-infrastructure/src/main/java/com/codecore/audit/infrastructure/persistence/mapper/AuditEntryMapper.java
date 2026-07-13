package com.codecore.audit.infrastructure.persistence.mapper;

import com.codecore.audit.domain.model.auditentry.AuditEntry;
import com.codecore.audit.domain.valueobject.ActionCode;
import com.codecore.audit.domain.valueobject.AuditEntryId;
import com.codecore.audit.domain.valueobject.AuditOutcome;
import com.codecore.audit.domain.valueobject.MembershipId;
import com.codecore.audit.domain.valueobject.ResourceId;
import com.codecore.audit.domain.valueobject.ResourceType;
import com.codecore.audit.domain.valueobject.TenantId;
import com.codecore.audit.infrastructure.persistence.entity.AuditEntryEntity;

/**
 * Isomorphic mapping between {@link AuditEntryEntity} and the {@link AuditEntry} aggregate (ADR-020).
 */
public final class AuditEntryMapper {

    public AuditEntry toDomain(AuditEntryEntity entity) {
        return AuditEntry.reconstitute(
                new AuditEntryId(entity.getAuditEntryId()),
                new TenantId(entity.getTenantId()),
                entity.getOccurredAt(),
                ActionCode.of(entity.getActionCode()),
                entity.getActorMembershipId() == null ? null : MembershipId.of(entity.getActorMembershipId()),
                ResourceType.of(entity.getResourceType()),
                ResourceId.of(entity.getResourceId()),
                AuditOutcome.valueOf(entity.getOutcome()),
                entity.getCreatedAt()
        );
    }

    public AuditEntryEntity toEntity(AuditEntry entry, boolean isNew) {
        AuditEntryEntity entity = new AuditEntryEntity();
        entity.setNewEntity(isNew);
        entity.setAuditEntryId(entry.id().value());
        entity.setTenantId(entry.tenantId().value());
        entity.setOccurredAt(entry.occurredAt());
        entity.setActionCode(entry.actionCode().value());
        entity.setActorMembershipId(entry.actorMembershipId().map(MembershipId::value).orElse(null));
        entity.setResourceType(entry.resourceType().value());
        entity.setResourceId(entry.resourceId().value());
        entity.setOutcome(entry.outcome().name());
        entity.setCreatedAt(entry.createdAt());
        return entity;
    }
}
