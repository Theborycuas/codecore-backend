package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.PermissionId;

import java.time.Instant;
import java.util.Objects;

public record AdminRolePermissionView(
        PermissionId permissionId,
        String code,
        String description,
        Instant assignedAt
) {

    public AdminRolePermissionView {
        Objects.requireNonNull(permissionId, "permissionId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }
}
