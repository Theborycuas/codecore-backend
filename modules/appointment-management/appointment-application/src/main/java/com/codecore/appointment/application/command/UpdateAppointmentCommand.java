package com.codecore.appointment.application.command;

import com.codecore.appointment.domain.valueobject.AppointmentId;

import java.time.Instant;
import java.util.UUID;

public record UpdateAppointmentCommand(
        AppointmentId appointmentId,
        UUID patientId,
        UUID staffAssignmentId,
        UUID organizationId,
        UUID officeId,
        Instant startsAt,
        Instant endsAt
) {
}
