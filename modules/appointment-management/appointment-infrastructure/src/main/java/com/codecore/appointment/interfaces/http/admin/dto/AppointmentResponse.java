package com.codecore.appointment.interfaces.http.admin.dto;

import com.codecore.appointment.application.dto.AdminAppointmentView;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientId,
        UUID staffAssignmentId,
        UUID organizationId,
        UUID officeId,
        Instant startsAt,
        Instant endsAt,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static AppointmentResponse from(AdminAppointmentView view) {
        return new AppointmentResponse(
                view.id().value(),
                view.patientId().value(),
                view.staffAssignmentId().value(),
                view.organizationId().value(),
                view.officeUuid(),
                view.startsAt(),
                view.endsAt(),
                view.status().name(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
