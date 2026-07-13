package com.codecore.access.application.dto;

import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.domain.valueobject.InvitationStatus;
import com.codecore.access.domain.valueobject.MembershipId;
import com.codecore.access.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.UUID;

public record AdminInvitationView(
        InvitationId id,
        TenantId tenantId,
        String invitedEmail,
        String invitedRoleCode,
        MembershipId invitedByMembershipId,
        Instant expiresAt,
        InvitationStatus status,
        MembershipId resultingMembershipId,
        Instant createdAt,
        Instant updatedAt,
        Instant acceptedAt,
        Instant revokedAt
) {

    public UUID invitedByMembershipUuid() {
        return invitedByMembershipId.value();
    }

    public UUID resultingMembershipUuid() {
        return resultingMembershipId == null ? null : resultingMembershipId.value();
    }
}
