package com.codecore.encounter.interfaces.http.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateEncounterRequest(
        @NotNull UUID patientId,
        @NotNull UUID staffAssignmentId,
        @NotNull UUID organizationId,
        UUID officeId,
        UUID appointmentId,
        @NotNull Instant startedAt,
        Instant endedAt
) {
}
