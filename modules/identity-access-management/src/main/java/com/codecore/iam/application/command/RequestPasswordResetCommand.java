package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.TenantId;

/**
 * @param tenantId optional operational tenant; when null, identity home tenant is used
 */
public record RequestPasswordResetCommand(
        TenantId tenantId,
        EmailAddress email
) {
}
