package com.codecore.organization.interfaces.http.admin.dto;

import com.codecore.organization.application.dto.AdminStaffAssignmentView;

import java.time.Instant;
import java.util.UUID;

public record StaffAssignmentResponse(
        UUID id,
        UUID membershipId,
        UUID organizationId,
        UUID officeId,
        Instant createdAt,
        Instant updatedAt
) {

    public static StaffAssignmentResponse from(AdminStaffAssignmentView view) {
        return new StaffAssignmentResponse(
                view.id().value(),
                view.membershipId().value(),
                view.organizationId().value(),
                view.officeId() != null ? view.officeId().value() : null,
                view.createdAt(),
                view.updatedAt()
        );
    }
}
