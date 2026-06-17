package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;

public record UpdateAdminUserCommand(
        IdentityId identityId,
        IdentityStatus status,
        String email
) {
}
