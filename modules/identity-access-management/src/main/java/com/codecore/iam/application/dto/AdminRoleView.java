package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.RoleStatus;
import com.codecore.iam.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;

public record AdminRoleView(
        RoleId id,
        TenantId tenantId,
        String code,
        String name,
        RoleStatus status,
        boolean systemRole,
        Instant createdAt,
        Instant updatedAt
) {

    public AdminRoleView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
