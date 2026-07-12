package com.codecore.encounter.infrastructure.persistence.mapper;

import com.codecore.encounter.domain.model.encounter.Encounter;
import com.codecore.encounter.domain.valueobject.AppointmentId;
import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.EncounterStatus;
import com.codecore.encounter.domain.valueobject.EncounterTimeBounds;
import com.codecore.encounter.domain.valueobject.OfficeId;
import com.codecore.encounter.domain.valueobject.OrganizationId;
import com.codecore.encounter.domain.valueobject.PatientId;
import com.codecore.encounter.domain.valueobject.StaffAssignmentId;
import com.codecore.encounter.domain.valueobject.TenantId;
import com.codecore.encounter.infrastructure.persistence.entity.EncounterEntity;

/**
 * Isomorphic mapping between {@link EncounterEntity} and {@link Encounter}.
 */
public final class EncounterMapper {

    public Encounter toDomain(EncounterEntity entity) {
        OfficeId officeId = entity.getOfficeId() == null
                ? null
                : OfficeId.of(entity.getOfficeId());
        AppointmentId appointmentId = entity.getAppointmentId() == null
                ? null
                : AppointmentId.of(entity.getAppointmentId());
        EncounterTimeBounds timeBounds = entity.getEndedAt() == null
                ? EncounterTimeBounds.open(entity.getStartedAt())
                : EncounterTimeBounds.of(entity.getStartedAt(), entity.getEndedAt());

        return Encounter.reconstitute(
                new EncounterId(entity.getEncounterId()),
                new TenantId(entity.getTenantId()),
                PatientId.of(entity.getPatientId()),
                StaffAssignmentId.of(entity.getStaffAssignmentId()),
                OrganizationId.of(entity.getOrganizationId()),
                officeId,
                appointmentId,
                timeBounds,
                EncounterStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public EncounterEntity toEntity(Encounter encounter, boolean isNew) {
        EncounterEntity entity = new EncounterEntity();
        entity.setNewEntity(isNew);
        entity.setEncounterId(encounter.id().value());
        entity.setTenantId(encounter.tenantId().value());
        entity.setPatientId(encounter.patientId().value());
        entity.setStaffAssignmentId(encounter.staffAssignmentId().value());
        entity.setOrganizationId(encounter.organizationId().value());
        entity.setOfficeId(encounter.officeId().map(OfficeId::value).orElse(null));
        entity.setAppointmentId(encounter.appointmentId().map(AppointmentId::value).orElse(null));
        entity.setStartedAt(encounter.startedAt());
        entity.setEndedAt(encounter.endedAt().orElse(null));
        entity.setStatus(encounter.status().name());
        entity.setCreatedAt(encounter.createdAt());
        entity.setUpdatedAt(encounter.updatedAt());
        return entity;
    }
}
