package com.codecore.organization.domain.model.staffassignment;

import com.codecore.organization.domain.exception.InvalidStaffAssignmentScopeException;
import com.codecore.organization.domain.valueobject.MembershipId;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import com.codecore.organization.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;

/**
 * StaffAssignment aggregate root — links IAM membership to org/office operational scope (ADR-010).
 */
public final class StaffAssignment {

    private final StaffAssignmentId id;
    private final TenantId tenantId;
    private final MembershipId membershipId;
    private OrganizationId organizationId;
    private OfficeId officeId;
    private final Instant createdAt;
    private Instant updatedAt;

    private StaffAssignment(
            StaffAssignmentId id,
            TenantId tenantId,
            MembershipId membershipId,
            OrganizationId organizationId,
            OfficeId officeId,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.membershipId = Objects.requireNonNull(membershipId, "membershipId");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
        this.officeId = officeId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static StaffAssignment create(
            StaffAssignmentId id,
            TenantId tenantId,
            MembershipId membershipId,
            OrganizationId organizationId,
            OfficeId officeId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new StaffAssignment(id, tenantId, membershipId, organizationId, officeId, now, now);
    }

    public static StaffAssignment reconstitute(
            StaffAssignmentId id,
            TenantId tenantId,
            MembershipId membershipId,
            OrganizationId organizationId,
            OfficeId officeId,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new StaffAssignment(
                id,
                tenantId,
                membershipId,
                organizationId,
                officeId,
                createdAt,
                updatedAt
        );
    }

    public StaffAssignmentId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public MembershipId membershipId() {
        return membershipId;
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    public OfficeId officeId() {
        return officeId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void changeScope(OrganizationId newOrganizationId, OfficeId newOfficeId) {
        this.organizationId = Objects.requireNonNull(newOrganizationId, "newOrganizationId");
        this.officeId = newOfficeId;
        touch();
    }

    public boolean isOrganizationLevel() {
        return officeId == null;
    }

    public static void assertOfficeBelongsToOrganization(
            OrganizationId organizationId,
            OfficeId officeId,
            OrganizationId officeOrganizationId
    ) {
        if (officeId != null && !organizationId.equals(officeOrganizationId)) {
            throw new InvalidStaffAssignmentScopeException(
                    "Office does not belong to the specified organization");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
