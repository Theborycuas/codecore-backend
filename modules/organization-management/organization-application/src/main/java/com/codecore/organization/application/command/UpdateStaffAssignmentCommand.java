package com.codecore.organization.application.command;

import com.codecore.organization.domain.valueobject.StaffAssignmentId;

import java.util.UUID;

public record UpdateStaffAssignmentCommand(
        StaffAssignmentId assignmentId,
        UUID organizationId,
        UUID officeId
) {
}
