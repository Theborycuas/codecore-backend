package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.TenantId;

/**
 * Inbound command to authenticate a tenant-scoped identity (email + password).
 */
public record AuthenticationCommand(
        TenantId tenantId,
        String email,
        String rawPassword
) {
}
