package com.codecore.iam.domain.model.membership;

import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Association aggregate: links one {@link IdentityId} to one {@link TenantId} (N:M across rows).
 * Role assignments (14.4) are owned internally via {@link MembershipRoleAssignment}.
 */
public final class IdentityTenantMembership {

    private final MembershipId id;
    private final IdentityId identityId;
    private final TenantId tenantId;
    private MembershipStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Set<MembershipRoleAssignment> roleAssignments;

    public IdentityTenantMembership(
            MembershipId id,
            IdentityId identityId,
            TenantId tenantId,
            MembershipStatus status,
            Instant createdAt,
            Instant updatedAt,
            Set<MembershipRoleAssignment> roleAssignments
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.identityId = Objects.requireNonNull(identityId, "identityId");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.roleAssignments = copyAssignments(roleAssignments);
    }

    public static IdentityTenantMembership create(
            IdentityId identityId,
            TenantId tenantId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new IdentityTenantMembership(
                MembershipId.generate(),
                identityId,
                tenantId,
                MembershipStatus.ACTIVE,
                now,
                now,
                Set.of()
        );
    }

    public static IdentityTenantMembership reconstitute(
            MembershipId id,
            IdentityId identityId,
            TenantId tenantId,
            MembershipStatus status,
            Instant createdAt,
            Instant updatedAt,
            Set<MembershipRoleAssignment> roleAssignments
    ) {
        return new IdentityTenantMembership(
                id,
                identityId,
                tenantId,
                status,
                createdAt,
                updatedAt,
                roleAssignments
        );
    }

    public MembershipId id() {
        return id;
    }

    public IdentityId identityId() {
        return identityId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public MembershipStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Set<MembershipRoleAssignment> roleAssignments() {
        return Collections.unmodifiableSet(roleAssignments);
    }

    public Set<RoleId> assignedRoleIds() {
        return roleAssignments.stream()
                .map(MembershipRoleAssignment::roleId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasRole(RoleId roleId) {
        Objects.requireNonNull(roleId, "roleId");
        return roleAssignments.stream().anyMatch(assignment -> assignment.roleId().equals(roleId));
    }

    public void assignRole(RoleId roleId, TenantId roleTenantId, Instant now) {
        Objects.requireNonNull(roleId, "roleId");
        Objects.requireNonNull(roleTenantId, "roleTenantId");
        Objects.requireNonNull(now, "now");
        ensureRoleTenantMatches(roleTenantId);
        MembershipRoleAssignment assignment = MembershipRoleAssignment.assign(roleId, now);
        if (!roleAssignments.add(assignment)) {
            throw new IllegalArgumentException("Role already assigned to membership");
        }
        touch();
    }

    public void revokeRole(RoleId roleId) {
        Objects.requireNonNull(roleId, "roleId");
        if (!roleAssignments.removeIf(assignment -> assignment.roleId().equals(roleId))) {
            throw new IllegalArgumentException("Role is not assigned to membership");
        }
        touch();
    }

    public void deactivate() {
        this.status = MembershipStatus.INACTIVE;
        touch();
    }

    public void activate() {
        this.status = MembershipStatus.ACTIVE;
        touch();
    }

    private void ensureRoleTenantMatches(TenantId roleTenantId) {
        if (!tenantId.equals(roleTenantId)) {
            throw new IllegalArgumentException("Role tenant must match membership tenant");
        }
    }

    private static Set<MembershipRoleAssignment> copyAssignments(Set<MembershipRoleAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(assignments);
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
