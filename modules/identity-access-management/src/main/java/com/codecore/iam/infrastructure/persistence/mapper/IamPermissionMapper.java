package com.codecore.iam.infrastructure.persistence.mapper;

import com.codecore.iam.domain.model.permission.Permission;
import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.PermissionId;
import com.codecore.iam.infrastructure.persistence.entity.IamPermissionEntity;

/**
 * Isomorphic mapping between {@link IamPermissionEntity} and {@link Permission}.
 */
public final class IamPermissionMapper {

    public Permission toDomain(IamPermissionEntity entity) {
        return new Permission(
                new PermissionId(entity.getPermissionId()),
                PermissionCode.of(entity.getCode()),
                entity.getDescription(),
                entity.isSystemPermission(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public IamPermissionEntity toEntity(Permission permission, boolean isNew) {
        IamPermissionEntity entity = new IamPermissionEntity();
        entity.setNewEntity(isNew);
        entity.setPermissionId(permission.id().value());
        entity.setCode(permission.code().value());
        entity.setDescription(permission.description());
        entity.setSystemPermission(permission.systemPermission());
        entity.setCreatedAt(permission.createdAt());
        entity.setUpdatedAt(permission.updatedAt());
        return entity;
    }
}
