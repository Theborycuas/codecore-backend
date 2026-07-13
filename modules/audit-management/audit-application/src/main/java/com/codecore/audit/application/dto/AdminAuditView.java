package com.codecore.audit.application.dto;

import com.codecore.audit.domain.valueobject.ActionCode;
import com.codecore.audit.domain.valueobject.AuditEntryId;
import com.codecore.audit.domain.valueobject.AuditOutcome;
import com.codecore.audit.domain.valueobject.MembershipId;
import com.codecore.audit.domain.valueobject.ResourceId;
import com.codecore.audit.domain.valueobject.ResourceType;
import com.codecore.audit.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditView(
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

    public UUID actorMembershipUuidOrNull() {
        return actorMembershipId == null ? null : actorMembershipId.value();
    }

    public UUID resourceUuid() {
        return resourceId.value();
    }
}
