package com.codecore.organization.application.dto;

import com.codecore.organization.domain.valueobject.MembershipId;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import com.codecore.organization.domain.valueobject.TenantId;

import java.time.Instant;

public record AdminStaffAssignmentView(
        StaffAssignmentId id,
        TenantId tenantId,
        MembershipId membershipId,
        OrganizationId organizationId,
        OfficeId officeId,
        Instant createdAt,
        Instant updatedAt
) {
}
