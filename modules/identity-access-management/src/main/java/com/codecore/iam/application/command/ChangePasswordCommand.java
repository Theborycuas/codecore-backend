package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.RawPassword;
import com.codecore.iam.domain.valueobject.TenantId;

public record ChangePasswordCommand(
        TenantId tenantId,
        IdentityId identityId,
        RawPassword currentPassword,
        RawPassword newPassword
) {
}
