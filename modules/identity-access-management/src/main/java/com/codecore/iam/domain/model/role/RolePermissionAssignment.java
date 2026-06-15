package com.codecore.iam.domain.model.role;

import com.codecore.iam.domain.valueobject.PermissionId;

import java.time.Instant;
import java.util.Objects;

/**
 * Internal entity of the {@link Role} aggregate — links one global permission to the role.
 * Not an aggregate root.
 */
public final class RolePermissionAssignment {

    private final PermissionId permissionId;
    private final Instant assignedAt;

    public RolePermissionAssignment(PermissionId permissionId, Instant assignedAt) {
        this.permissionId = Objects.requireNonNull(permissionId, "permissionId");
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt");
    }

    public static RolePermissionAssignment assign(PermissionId permissionId, Instant now) {
        Objects.requireNonNull(now, "now");
        return new RolePermissionAssignment(permissionId, now);
    }

    public PermissionId permissionId() {
        return permissionId;
    }

    public Instant assignedAt() {
        return assignedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        RolePermissionAssignment that = (RolePermissionAssignment) other;
        return permissionId.equals(that.permissionId);
    }

    @Override
    public int hashCode() {
        return permissionId.hashCode();
    }
}
