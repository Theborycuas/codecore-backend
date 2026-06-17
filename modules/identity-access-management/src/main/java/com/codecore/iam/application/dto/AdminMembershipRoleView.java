package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.RoleId;

import java.time.Instant;
import java.util.Objects;

public record AdminMembershipRoleView(
        RoleId roleId,
        String code,
        String name,
        String status,
        boolean systemRole,
        Instant assignedAt
) {

    public AdminMembershipRoleView {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }
}
