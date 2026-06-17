package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.IdentityStatus;

public record CreateAdminUserCommand(
        String email,
        String rawPassword,
        IdentityStatus initialStatus
) {
}
