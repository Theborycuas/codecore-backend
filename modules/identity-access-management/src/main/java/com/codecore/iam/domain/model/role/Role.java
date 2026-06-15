package com.codecore.iam.domain.model.role;

import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.RoleStatus;
import com.codecore.iam.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;

/**
 * Tenant-scoped role aggregate root — groups permissions (14.3) and is assigned via membership (14.4).
 */
public final class Role {

    private final RoleId id;
    private final TenantId tenantId;
    private final RoleCode code;
    private RoleName name;
    private RoleStatus status;
    private final boolean systemRole;
    private final Instant createdAt;
    private Instant updatedAt;

    public Role(
            RoleId id,
            TenantId tenantId,
            RoleCode code,
            RoleName name,
            RoleStatus status,
            boolean systemRole,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.status = Objects.requireNonNull(status, "status");
        this.systemRole = systemRole;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Role create(
            TenantId tenantId,
            RoleCode code,
            RoleName name,
            Instant now
    ) {
        return create(tenantId, code, name, false, now);
    }

    public static Role createSystemRole(
            TenantId tenantId,
            RoleCode code,
            RoleName name,
            Instant now
    ) {
        return create(tenantId, code, name, true, now);
    }

    private static Role create(
            TenantId tenantId,
            RoleCode code,
            RoleName name,
            boolean systemRole,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new Role(
                RoleId.generate(),
                tenantId,
                code,
                name,
                RoleStatus.ACTIVE,
                systemRole,
                now,
                now
        );
    }

    public RoleId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public RoleCode code() {
        return code;
    }

    public RoleName name() {
        return name;
    }

    public RoleStatus status() {
        return status;
    }

    public boolean systemRole() {
        return systemRole;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void rename(RoleName newName) {
        Objects.requireNonNull(newName, "newName");
        ensureMutable();
        this.name = newName;
        touch();
    }

    public void deactivate() {
        ensureMutable();
        this.status = RoleStatus.INACTIVE;
        touch();
    }

    public void activate() {
        ensureMutable();
        this.status = RoleStatus.ACTIVE;
        touch();
    }

    private void ensureMutable() {
        if (systemRole) {
            throw new IllegalStateException("System roles cannot be modified");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
