package com.codecore.access.infrastructure.persistence.mapper;

import com.codecore.access.domain.model.invitation.Invitation;
import com.codecore.access.domain.valueobject.EmailAddress;
import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.domain.valueobject.InvitationRoleCode;
import com.codecore.access.domain.valueobject.InvitationStatus;
import com.codecore.access.domain.valueobject.InvitationTokenHash;
import com.codecore.access.domain.valueobject.MembershipId;
import com.codecore.access.domain.valueobject.TenantId;
import com.codecore.access.infrastructure.persistence.entity.InvitationEntity;

/**
 * Isomorphic mapping between {@link InvitationEntity} and the {@link Invitation} aggregate (ADR-019).
 */
public final class InvitationMapper {

    public Invitation toDomain(InvitationEntity entity) {
        return Invitation.reconstitute(
                new InvitationId(entity.getInvitationId()),
                new TenantId(entity.getTenantId()),
                EmailAddress.of(entity.getInvitedEmail()),
                InvitationRoleCode.of(entity.getInvitedRoleCode()),
                MembershipId.of(entity.getInvitedByMembershipId()),
                InvitationTokenHash.ofHashedValue(entity.getTokenHash()),
                entity.getExpiresAt(),
                InvitationStatus.valueOf(entity.getStatus()),
                entity.getResultingMembershipId() == null
                        ? null
                        : MembershipId.of(entity.getResultingMembershipId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getAcceptedAt(),
                entity.getRevokedAt()
        );
    }

    public InvitationEntity toEntity(Invitation invitation, boolean isNew) {
        InvitationEntity entity = new InvitationEntity();
        entity.setNewEntity(isNew);
        entity.setInvitationId(invitation.id().value());
        entity.setTenantId(invitation.tenantId().value());
        entity.setInvitedEmail(invitation.invitedEmail().value());
        entity.setInvitedRoleCode(invitation.invitedRoleCode().value());
        entity.setInvitedByMembershipId(invitation.invitedByMembershipId().value());
        entity.setTokenHash(invitation.tokenHash().value());
        entity.setExpiresAt(invitation.expiresAt());
        entity.setStatus(invitation.status().name());
        entity.setResultingMembershipId(invitation.resultingMembershipId().map(MembershipId::value).orElse(null));
        entity.setCreatedAt(invitation.createdAt());
        entity.setUpdatedAt(invitation.updatedAt());
        entity.setAcceptedAt(invitation.acceptedAt().orElse(null));
        entity.setRevokedAt(invitation.revokedAt().orElse(null));
        return entity;
    }
}
