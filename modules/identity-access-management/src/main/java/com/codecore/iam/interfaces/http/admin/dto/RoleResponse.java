package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.application.dto.AdminRoleView;

import java.time.Instant;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        UUID tenantId,
        String code,
        String name,
        String status,
        boolean systemRole,
        Instant createdAt,
        Instant updatedAt
) {

    public static RoleResponse from(AdminRoleView view) {
        return new RoleResponse(
                view.id().value(),
                view.tenantId().value(),
                view.code(),
                view.name(),
                view.status().name(),
                view.systemRole(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
