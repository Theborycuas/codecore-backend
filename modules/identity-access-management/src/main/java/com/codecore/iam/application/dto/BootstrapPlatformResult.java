package com.codecore.iam.application.dto;

import com.codecore.iam.domain.valueobject.TenantId;

/**
 * Result of greenfield platform bootstrap (PASO 15.9.2).
 */
public record BootstrapPlatformResult(
        TenantId tenantId,
        String tenantName,
        String ownerEmail,
        boolean executed
) {

    public static BootstrapPlatformResult skipped() {
        return new BootstrapPlatformResult(null, null, null, false);
    }

    public static BootstrapPlatformResult completed(TenantId tenantId, String tenantName, String ownerEmail) {
        return new BootstrapPlatformResult(tenantId, tenantName, ownerEmail, true);
    }
}
