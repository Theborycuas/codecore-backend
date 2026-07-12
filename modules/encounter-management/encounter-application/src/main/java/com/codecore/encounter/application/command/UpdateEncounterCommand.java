package com.codecore.encounter.application.command;

import com.codecore.encounter.domain.valueobject.EncounterId;

import java.time.Instant;
import java.util.UUID;

public record UpdateEncounterCommand(
        EncounterId encounterId,
        UUID patientId,
        UUID staffAssignmentId,
        UUID organizationId,
        UUID officeId,
        UUID appointmentId,
        Instant startedAt,
        Instant endedAt
) {
}
