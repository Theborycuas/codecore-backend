package com.codecore.iam.infrastructure.persistence.mapper;

import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.RoleStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.persistence.entity.IamRoleEntity;

/**
 * Isomorphic mapping between {@link IamRoleEntity} and {@link Role}.
 */
public final class IamRoleMapper {

    public Role toDomain(IamRoleEntity entity) {
        return new Role(
                new RoleId(entity.getRoleId()),
                new TenantId(entity.getTenantId()),
                RoleCode.of(entity.getCode()),
                RoleName.of(entity.getName()),
                RoleStatus.valueOf(entity.getStatus()),
                entity.isSystemRole(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public IamRoleEntity toEntity(Role role, boolean isNew) {
        IamRoleEntity entity = new IamRoleEntity();
        entity.setNewEntity(isNew);
        entity.setRoleId(role.id().value());
        entity.setTenantId(role.tenantId().value());
        entity.setCode(role.code().value());
        entity.setName(role.name().value());
        entity.setStatus(role.status().name());
        entity.setSystemRole(role.systemRole());
        entity.setCreatedAt(role.createdAt());
        entity.setUpdatedAt(role.updatedAt());
        return entity;
    }
}
