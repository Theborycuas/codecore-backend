package com.codecore.iam.application.command;

public record CreateAdminRoleCommand(
        String code,
        String name
) {
}
