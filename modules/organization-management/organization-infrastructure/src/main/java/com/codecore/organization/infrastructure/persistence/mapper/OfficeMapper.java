package com.codecore.organization.infrastructure.persistence.mapper;

import com.codecore.organization.domain.model.office.Office;
import com.codecore.organization.domain.valueobject.OfficeCode;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OfficeName;
import com.codecore.organization.domain.valueobject.OfficeStatus;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.TenantId;
import com.codecore.organization.infrastructure.persistence.entity.OfficeEntity;

public final class OfficeMapper {

    public Office toDomain(OfficeEntity entity) {
        return Office.reconstitute(
                new OfficeId(entity.getOfficeId()),
                new TenantId(entity.getTenantId()),
                new OrganizationId(entity.getOrganizationId()),
                OfficeCode.of(entity.getCode()),
                OfficeName.of(entity.getName()),
                OfficeStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public OfficeEntity toEntity(Office office, boolean isNew) {
        OfficeEntity entity = new OfficeEntity();
        entity.setNewEntity(isNew);
        entity.setOfficeId(office.id().value());
        entity.setTenantId(office.tenantId().value());
        entity.setOrganizationId(office.organizationId().value());
        entity.setCode(office.code().value());
        entity.setName(office.name().value());
        entity.setStatus(office.status().name());
        entity.setCreatedAt(office.createdAt());
        entity.setUpdatedAt(office.updatedAt());
        return entity;
    }
}
