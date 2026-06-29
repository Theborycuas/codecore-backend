package com.codecore.organization.interfaces.http.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateStaffAssignmentRequest(
        @NotNull UUID organizationId,
        UUID officeId
) {
}
