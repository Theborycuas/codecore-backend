package com.codecore.access.interfaces.http.admin.dto;

import com.codecore.access.application.dto.AdminInvitationView;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID tenantId,
        String invitedEmail,
        String invitedRoleCode,
        UUID invitedByMembershipId,
        Instant expiresAt,
        String status,
        UUID resultingMembershipId,
        Instant createdAt,
        Instant updatedAt,
        Instant acceptedAt,
        Instant revokedAt
) {

    public static InvitationResponse from(AdminInvitationView view) {
        return new InvitationResponse(
                view.id().value(),
                view.tenantId().value(),
                view.invitedEmail(),
                view.invitedRoleCode(),
                view.invitedByMembershipUuid(),
                view.expiresAt(),
                view.status().name(),
                view.resultingMembershipUuid(),
                view.createdAt(),
                view.updatedAt(),
                view.acceptedAt(),
                view.revokedAt()
        );
    }
}
