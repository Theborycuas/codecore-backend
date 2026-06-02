package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.TenantId;

/**
 * Inbound command to register a new tenant-scoped identity (email-first).
 */
public record RegisterIdentityCommand(
        TenantId tenantId,
        String email,
        String rawPassword
) {
}
