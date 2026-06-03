package com.codecore.iam.infrastructure.persistence.mapper;

import com.codecore.iam.domain.model.tenant.Tenant;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantName;
import com.codecore.iam.domain.valueobject.TenantStatus;
import com.codecore.iam.infrastructure.persistence.entity.IamTenantEntity;

/**
 * Isomorphic mapping between {@link IamTenantEntity} and {@link Tenant}.
 */
public final class IamTenantMapper {

    public Tenant toDomain(IamTenantEntity entity) {
        return new Tenant(
                new TenantId(entity.getTenantId()),
                TenantName.of(entity.getName()),
                TenantStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public IamTenantEntity toEntity(Tenant tenant, boolean isNew) {
        IamTenantEntity entity = new IamTenantEntity();
        entity.setNewEntity(isNew);
        entity.setTenantId(tenant.id().value());
        entity.setName(tenant.name().value());
        entity.setStatus(tenant.status().name());
        entity.setCreatedAt(tenant.createdAt());
        entity.setUpdatedAt(tenant.updatedAt());
        return entity;
    }
}
