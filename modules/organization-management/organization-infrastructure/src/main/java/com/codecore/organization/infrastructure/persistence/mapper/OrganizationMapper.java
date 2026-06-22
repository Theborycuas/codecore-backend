package com.codecore.organization.infrastructure.persistence.mapper;

import com.codecore.organization.domain.model.organization.Organization;
import com.codecore.organization.domain.valueobject.OrganizationCode;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.OrganizationName;
import com.codecore.organization.domain.valueobject.OrganizationStatus;
import com.codecore.organization.domain.valueobject.TenantId;
import com.codecore.organization.infrastructure.persistence.entity.OrganizationEntity;

/**
 * Isomorphic mapping between {@link OrganizationEntity} and {@link Organization}.
 */
public final class OrganizationMapper {

    public Organization toDomain(OrganizationEntity entity) {
        return Organization.reconstitute(
                new OrganizationId(entity.getOrganizationId()),
                new TenantId(entity.getTenantId()),
                OrganizationCode.of(entity.getCode()),
                OrganizationName.of(entity.getName()),
                OrganizationStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public OrganizationEntity toEntity(Organization organization, boolean isNew) {
        OrganizationEntity entity = new OrganizationEntity();
        entity.setNewEntity(isNew);
        entity.setOrganizationId(organization.id().value());
        entity.setTenantId(organization.tenantId().value());
        entity.setCode(organization.code().value());
        entity.setName(organization.name().value());
        entity.setStatus(organization.status().name());
        entity.setCreatedAt(organization.createdAt());
        entity.setUpdatedAt(organization.updatedAt());
        return entity;
    }
}
