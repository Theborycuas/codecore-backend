package com.codecore.iam.domain.valueobject;

import java.util.UUID;

/**
 * Tenant ownership semantics for tenant-aware IAM aggregates (blueprint: TenantIdentifier).
 */
public final class TenantId extends UuidIdentifier {

    public TenantId(UUID value) {
        super(value);
    }

    public TenantId(String value) {
        super(value);
    }

    public static TenantId generate() {
        return new TenantId(UUID.randomUUID());
    }
}
