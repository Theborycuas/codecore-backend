package com.codecore.appointment.application.dto;

import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.AppointmentStatus;
import com.codecore.appointment.domain.valueobject.OfficeId;
import com.codecore.appointment.domain.valueobject.OrganizationId;
import com.codecore.appointment.domain.valueobject.PatientId;
import com.codecore.appointment.domain.valueobject.StaffAssignmentId;
import com.codecore.appointment.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.UUID;

public record AdminAppointmentView(
        AppointmentId id,
        TenantId tenantId,
        PatientId patientId,
        StaffAssignmentId staffAssignmentId,
        OrganizationId organizationId,
        OfficeId officeId,
        Instant startsAt,
        Instant endsAt,
        AppointmentStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public UUID officeUuid() {
        return officeId == null ? null : officeId.value();
    }
}
