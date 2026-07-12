package com.codecore.encounter.application.command;

import java.time.Instant;
import java.util.UUID;

public record CreateEncounterCommand(
        UUID patientId,
        UUID staffAssignmentId,
        UUID organizationId,
        UUID officeId,
        UUID appointmentId,
        Instant startedAt,
        Instant endedAt
) {
}
