package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.PermissionId;

import java.time.Instant;
import java.util.Objects;

public record AdminPermissionView(
        PermissionId id,
        String code,
        String description,
        boolean systemPermission,
        Instant createdAt,
        Instant updatedAt
) {

    public AdminPermissionView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
