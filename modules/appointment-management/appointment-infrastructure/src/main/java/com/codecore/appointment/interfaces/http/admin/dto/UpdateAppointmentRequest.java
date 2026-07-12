package com.codecore.appointment.interfaces.http.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record UpdateAppointmentRequest(
        @NotNull UUID patientId,
        @NotNull UUID staffAssignmentId,
        @NotNull UUID organizationId,
        UUID officeId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt
) {
}
