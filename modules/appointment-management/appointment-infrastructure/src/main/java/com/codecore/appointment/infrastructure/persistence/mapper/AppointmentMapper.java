package com.codecore.appointment.infrastructure.persistence.mapper;

import com.codecore.appointment.domain.model.appointment.Appointment;
import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.AppointmentStatus;
import com.codecore.appointment.domain.valueobject.AppointmentTimeWindow;
import com.codecore.appointment.domain.valueobject.OfficeId;
import com.codecore.appointment.domain.valueobject.OrganizationId;
import com.codecore.appointment.domain.valueobject.PatientId;
import com.codecore.appointment.domain.valueobject.StaffAssignmentId;
import com.codecore.appointment.domain.valueobject.TenantId;
import com.codecore.appointment.infrastructure.persistence.entity.AppointmentEntity;

/**
 * Isomorphic mapping between {@link AppointmentEntity} and {@link Appointment}.
 */
public final class AppointmentMapper {

    public Appointment toDomain(AppointmentEntity entity) {
        OfficeId officeId = entity.getOfficeId() == null
                ? null
                : OfficeId.of(entity.getOfficeId());

        return Appointment.reconstitute(
                new AppointmentId(entity.getAppointmentId()),
                new TenantId(entity.getTenantId()),
                PatientId.of(entity.getPatientId()),
                StaffAssignmentId.of(entity.getStaffAssignmentId()),
                OrganizationId.of(entity.getOrganizationId()),
                officeId,
                AppointmentTimeWindow.of(entity.getStartsAt(), entity.getEndsAt()),
                AppointmentStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public AppointmentEntity toEntity(Appointment appointment, boolean isNew) {
        AppointmentEntity entity = new AppointmentEntity();
        entity.setNewEntity(isNew);
        entity.setAppointmentId(appointment.id().value());
        entity.setTenantId(appointment.tenantId().value());
        entity.setPatientId(appointment.patientId().value());
        entity.setStaffAssignmentId(appointment.staffAssignmentId().value());
        entity.setOrganizationId(appointment.organizationId().value());
        entity.setOfficeId(appointment.officeId().map(OfficeId::value).orElse(null));
        entity.setStartsAt(appointment.startsAt());
        entity.setEndsAt(appointment.endsAt());
        entity.setStatus(appointment.status().name());
        entity.setCreatedAt(appointment.createdAt());
        entity.setUpdatedAt(appointment.updatedAt());
        return entity;
    }
}
