package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.TenantId;

public record RequestPasswordResetCommand(
        TenantId tenantId,
        EmailAddress email
) {
}
