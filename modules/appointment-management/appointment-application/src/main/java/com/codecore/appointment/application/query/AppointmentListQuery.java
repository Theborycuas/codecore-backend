package com.codecore.appointment.application.query;

import java.time.Instant;
import java.util.UUID;

public record AppointmentListQuery(
        AppointmentListFilter status,
        UUID organizationId,
        UUID patientId,
        UUID staffAssignmentId,
        UUID officeId,
        Instant from,
        Instant to
) {

    public static AppointmentListQuery of(
            String status,
            UUID organizationId,
            UUID patientId,
            UUID staffAssignmentId,
            UUID officeId,
            Instant from,
            Instant to
    ) {
        return new AppointmentListQuery(
                AppointmentListFilter.parse(status),
                organizationId,
                patientId,
                staffAssignmentId,
                officeId,
                from,
                to
        );
    }
}
