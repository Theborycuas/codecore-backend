package com.codecore.encounter.application.dto;

import com.codecore.encounter.domain.valueobject.AppointmentId;
import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.EncounterStatus;
import com.codecore.encounter.domain.valueobject.OfficeId;
import com.codecore.encounter.domain.valueobject.OrganizationId;
import com.codecore.encounter.domain.valueobject.PatientId;
import com.codecore.encounter.domain.valueobject.StaffAssignmentId;
import com.codecore.encounter.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.UUID;

public record AdminEncounterView(
        EncounterId id,
        TenantId tenantId,
        PatientId patientId,
        StaffAssignmentId staffAssignmentId,
        OrganizationId organizationId,
        OfficeId officeId,
        AppointmentId appointmentId,
        Instant startedAt,
        Instant endedAt,
        EncounterStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public UUID officeUuid() {
        return officeId == null ? null : officeId.value();
    }

    public UUID appointmentUuid() {
        return appointmentId == null ? null : appointmentId.value();
    }
}
