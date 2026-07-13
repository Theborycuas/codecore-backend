package com.codecore.audit.interfaces.http.admin.dto;

import com.codecore.audit.application.dto.AdminAuditView;

import java.time.Instant;
import java.util.UUID;

public record AuditEntryResponse(
        UUID id,
        UUID tenantId,
        Instant occurredAt,
        String actionCode,
        UUID actorMembershipId,
        String resourceType,
        UUID resourceId,
        String outcome,
        Instant createdAt
) {

    public static AuditEntryResponse from(AdminAuditView view) {
        return new AuditEntryResponse(
                view.id().value(),
                view.tenantId().value(),
                view.occurredAt(),
                view.actionCode().value(),
                view.actorMembershipUuidOrNull(),
                view.resourceType().value(),
                view.resourceUuid(),
                view.outcome().name(),
                view.createdAt()
        );
    }
}
