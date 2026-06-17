package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.domain.valueobject.RoleStatus;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @Size(max = 200) String name,
        RoleStatus status
) {
}
