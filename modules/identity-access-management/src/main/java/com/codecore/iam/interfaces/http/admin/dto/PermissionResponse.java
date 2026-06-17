package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.application.dto.AdminPermissionView;

import java.time.Instant;
import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String code,
        String description,
        boolean systemPermission,
        Instant createdAt,
        Instant updatedAt
) {

    public static PermissionResponse from(AdminPermissionView view) {
        return new PermissionResponse(
                view.id().value(),
                view.code(),
                view.description(),
                view.systemPermission(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
