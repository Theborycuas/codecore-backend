package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.RoleStatus;

public record UpdateAdminRoleCommand(
        RoleId roleId,
        String name,
        RoleStatus status
) {
}
