package com.codecore.iam.domain.model.common;

import com.codecore.iam.domain.valueobject.TenantId;

import java.util.Objects;

/**
 * Shared tenant ownership and optimistic versioning for IAM aggregate roots.
 */
public abstract class AggregateRoot {

    private final TenantId tenantId;
    private long version;

    protected AggregateRoot(TenantId tenantId) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
    }

    protected AggregateRoot(TenantId tenantId, long version) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.version = version;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public long version() {
        return version;
    }

    protected void bumpVersion() {
        version++;
    }
}
