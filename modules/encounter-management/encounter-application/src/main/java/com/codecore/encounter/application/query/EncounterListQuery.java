package com.codecore.encounter.application.query;

import java.time.Instant;
import java.util.UUID;

public record EncounterListQuery(
        EncounterListFilter status,
        UUID organizationId,
        UUID patientId,
        UUID staffAssignmentId,
        UUID officeId,
        UUID appointmentId,
        Instant from,
        Instant to
) {

    public static EncounterListQuery of(
            String status,
            UUID organizationId,
            UUID patientId,
            UUID staffAssignmentId,
            UUID officeId,
            UUID appointmentId,
            Instant from,
            Instant to
    ) {
        return new EncounterListQuery(
                EncounterListFilter.parse(status),
                organizationId,
                patientId,
                staffAssignmentId,
                officeId,
                appointmentId,
                from,
                to
        );
    }
}
