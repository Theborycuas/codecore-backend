package com.codecore.organization.contract.reference;

import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import com.codecore.organization.domain.valueobject.TenantId;

import java.util.Objects;
import java.util.Optional;

/**
 * Minimal immutable scope of a StaffAssignment for consumer write-time coherence (ADR-013).
 * Not an aggregate, entity, or admin DTO.
 */
public final class StaffAssignmentReferenceView {

    private final StaffAssignmentId staffAssignmentId;
    private final OrganizationId organizationId;
    private final OfficeId officeId;

    public StaffAssignmentReferenceView(
            StaffAssignmentId staffAssignmentId,
            OrganizationId organizationId,
            OfficeId officeId
    ) {
        this.staffAssignmentId = Objects.requireNonNull(staffAssignmentId, "staffAssignmentId");
        this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
        this.officeId = officeId;
    }

    public StaffAssignmentId staffAssignmentId() {
        return staffAssignmentId;
    }

    public OrganizationId organizationId() {
        return organizationId;
    }

    /**
     * Empty when the assignment is organization-wide (no fixed office).
     */
    public Optional<OfficeId> officeId() {
        return Optional.ofNullable(officeId);
    }

    public boolean isOrganizationWide() {
        return officeId == null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        StaffAssignmentReferenceView that = (StaffAssignmentReferenceView) other;
        return staffAssignmentId.equals(that.staffAssignmentId)
                && organizationId.equals(that.organizationId)
                && Objects.equals(officeId, that.officeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(staffAssignmentId, organizationId, officeId);
    }
}
