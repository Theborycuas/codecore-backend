package com.codecore.organization.application.query;

import com.codecore.organization.domain.valueobject.MembershipId;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;

import java.util.Optional;

public record StaffAssignmentListFilter(
        Optional<MembershipId> membershipId,
        Optional<OrganizationId> organizationId,
        Optional<OfficeId> officeId
) {

    public static StaffAssignmentListFilter of(
            MembershipId membershipId,
            OrganizationId organizationId,
            OfficeId officeId
    ) {
        return new StaffAssignmentListFilter(
                Optional.ofNullable(membershipId),
                Optional.ofNullable(organizationId),
                Optional.ofNullable(officeId)
        );
    }

    public static StaffAssignmentListFilter empty() {
        return new StaffAssignmentListFilter(Optional.empty(), Optional.empty(), Optional.empty());
    }
}
