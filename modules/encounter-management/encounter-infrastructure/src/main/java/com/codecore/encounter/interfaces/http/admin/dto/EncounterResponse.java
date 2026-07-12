package com.codecore.encounter.interfaces.http.admin.dto;

import com.codecore.encounter.application.dto.AdminEncounterView;

import java.time.Instant;
import java.util.UUID;

public record EncounterResponse(
        UUID id,
        UUID patientId,
        UUID staffAssignmentId,
        UUID organizationId,
        UUID officeId,
        UUID appointmentId,
        Instant startedAt,
        Instant endedAt,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static EncounterResponse from(AdminEncounterView view) {
        return new EncounterResponse(
                view.id().value(),
                view.patientId().value(),
                view.staffAssignmentId().value(),
                view.organizationId().value(),
                view.officeUuid(),
                view.appointmentUuid(),
                view.startedAt(),
                view.endedAt(),
                view.status().name(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
