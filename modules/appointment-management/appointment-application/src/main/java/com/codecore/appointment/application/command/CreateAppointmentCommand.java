package com.codecore.appointment.application.command;

import com.codecore.appointment.domain.valueobject.AppointmentId;

import java.time.Instant;
import java.util.UUID;

public record CreateAppointmentCommand(
        UUID patientId,
        UUID staffAssignmentId,
        UUID organizationId,
        UUID officeId,
        Instant startsAt,
        Instant endsAt
) {
}
