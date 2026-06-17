package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.RoleId;

import java.util.List;
import java.util.UUID;

public record ReplaceAdminRolePermissionsCommand(
        RoleId roleId,
        List<UUID> permissionIds
) {
}
