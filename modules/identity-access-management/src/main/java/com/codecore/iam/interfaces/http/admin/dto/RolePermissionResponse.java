package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.application.dto.AdminRolePermissionView;

import java.time.Instant;
import java.util.UUID;

public record RolePermissionResponse(
        UUID permissionId,
        String code,
        String description,
        Instant assignedAt
) {

    public static RolePermissionResponse from(AdminRolePermissionView view) {
        return new RolePermissionResponse(
                view.permissionId().value(),
                view.code(),
                view.description(),
                view.assignedAt()
        );
    }
}
