package com.codecore.iam.domain.model.membership;

import com.codecore.iam.domain.valueobject.RoleId;

import java.time.Instant;
import java.util.Objects;

/**
 * Internal entity of the {@link IdentityTenantMembership} aggregate — links one role to the membership.
 * Not an aggregate root.
 */
public final class MembershipRoleAssignment {

    private final RoleId roleId;
    private final Instant assignedAt;

    public MembershipRoleAssignment(RoleId roleId, Instant assignedAt) {
        this.roleId = Objects.requireNonNull(roleId, "roleId");
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt");
    }

    public static MembershipRoleAssignment assign(RoleId roleId, Instant now) {
        Objects.requireNonNull(now, "now");
        return new MembershipRoleAssignment(roleId, now);
    }

    public RoleId roleId() {
        return roleId;
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
        MembershipRoleAssignment that = (MembershipRoleAssignment) other;
        return roleId.equals(that.roleId);
    }

    @Override
    public int hashCode() {
        return roleId.hashCode();
    }
}
