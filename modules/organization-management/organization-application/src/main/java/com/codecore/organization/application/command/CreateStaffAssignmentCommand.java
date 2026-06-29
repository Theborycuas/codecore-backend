package com.codecore.organization.application.command;

import java.util.UUID;

public record CreateStaffAssignmentCommand(
        UUID membershipId,
        UUID organizationId,
        UUID officeId
) {
}
