package com.codecore.iam.domain.model.role;

import com.codecore.iam.domain.valueobject.PermissionId;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.RoleStatus;
import com.codecore.iam.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final Set<RolePermissionAssignment> permissionAssignments;

    public Role(
            RoleId id,
            TenantId tenantId,
            RoleCode code,
            RoleName name,
            RoleStatus status,
            boolean systemRole,
            Instant createdAt,
            Instant updatedAt,
            Set<RolePermissionAssignment> permissionAssignments
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.status = Objects.requireNonNull(status, "status");
        this.systemRole = systemRole;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.permissionAssignments = copyAssignments(permissionAssignments);
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

    public static Role reconstitute(
            RoleId id,
            TenantId tenantId,
            RoleCode code,
            RoleName name,
            RoleStatus status,
            boolean systemRole,
            Instant createdAt,
            Instant updatedAt,
            Set<RolePermissionAssignment> permissionAssignments
    ) {
        return new Role(
                id,
                tenantId,
                code,
                name,
                status,
                systemRole,
                createdAt,
                updatedAt,
                permissionAssignments
        );
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
                now,
                Set.of()
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

    public Set<RolePermissionAssignment> permissionAssignments() {
        return Collections.unmodifiableSet(permissionAssignments);
    }

    public Set<PermissionId> assignedPermissionIds() {
        return permissionAssignments.stream()
                .map(RolePermissionAssignment::permissionId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasPermission(PermissionId permissionId) {
        Objects.requireNonNull(permissionId, "permissionId");
        return permissionAssignments.stream()
                .anyMatch(assignment -> assignment.permissionId().equals(permissionId));
    }

    public void assignPermission(PermissionId permissionId, Instant now) {
        Objects.requireNonNull(permissionId, "permissionId");
        Objects.requireNonNull(now, "now");
        ensureMutable();
        RolePermissionAssignment assignment = RolePermissionAssignment.assign(permissionId, now);
        if (!permissionAssignments.add(assignment)) {
            throw new IllegalArgumentException("Permission already assigned to role");
        }
        touch();
    }

    public void revokePermission(PermissionId permissionId) {
        Objects.requireNonNull(permissionId, "permissionId");
        ensureMutable();
        if (!permissionAssignments.removeIf(assignment -> assignment.permissionId().equals(permissionId))) {
            throw new IllegalArgumentException("Permission is not assigned to role");
        }
        touch();
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

    private static Set<RolePermissionAssignment> copyAssignments(Set<RolePermissionAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(assignments);
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
