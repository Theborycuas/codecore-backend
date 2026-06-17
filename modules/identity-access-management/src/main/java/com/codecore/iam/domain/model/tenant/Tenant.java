package com.codecore.iam.domain.model.tenant;

import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantName;
import com.codecore.iam.domain.valueobject.TenantStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Tenant aggregate root — top-level multi-tenant boundary (not tenant-scoped under another tenant).
 */
public final class Tenant {

    private final TenantId id;
    private TenantName name;
    private TenantStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Tenant(
            TenantId id,
            TenantName name,
            TenantStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Tenant create(TenantId id, TenantName name, Instant now) {
        Objects.requireNonNull(now, "now");
        return new Tenant(id, name, TenantStatus.ACTIVE, now, now);
    }

    public TenantId id() {
        return id;
    }

    public TenantName name() {
        return name;
    }

    public TenantStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void suspend() {
        this.status = TenantStatus.SUSPENDED;
        touch();
    }

    public void disable() {
        this.status = TenantStatus.DISABLED;
        touch();
    }

    public void activate() {
        this.status = TenantStatus.ACTIVE;
        touch();
    }

    public void rename(TenantName newName) {
        this.name = Objects.requireNonNull(newName, "newName");
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
