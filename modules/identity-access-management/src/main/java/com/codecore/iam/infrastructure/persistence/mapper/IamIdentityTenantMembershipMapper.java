package com.codecore.iam.infrastructure.persistence.mapper;

import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.persistence.entity.IamIdentityTenantMembershipEntity;

import java.util.Set;

/**
 * Isomorphic mapping between {@link IamIdentityTenantMembershipEntity} and {@link IdentityTenantMembership}.
 */
public final class IamIdentityTenantMembershipMapper {

    public IdentityTenantMembership toDomain(IamIdentityTenantMembershipEntity entity) {
        return IdentityTenantMembership.reconstitute(
                new MembershipId(entity.getMembershipId()),
                new IdentityId(entity.getIdentityId()),
                new TenantId(entity.getTenantId()),
                MembershipStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                Set.of()
        );
    }

    public IamIdentityTenantMembershipEntity toEntity(IdentityTenantMembership membership, boolean isNew) {
        IamIdentityTenantMembershipEntity entity = new IamIdentityTenantMembershipEntity();
        entity.setNewEntity(isNew);
        entity.setMembershipId(membership.id().value());
        entity.setIdentityId(membership.identityId().value());
        entity.setTenantId(membership.tenantId().value());
        entity.setStatus(membership.status().name());
        entity.setCreatedAt(membership.createdAt());
        entity.setUpdatedAt(membership.updatedAt());
        return entity;
    }
}
