package com.codecore.appointment.infrastructure.persistence.repository;

import com.codecore.appointment.application.port.out.AppointmentQueryPort;
import com.codecore.appointment.application.port.out.AppointmentRepository;
import com.codecore.appointment.domain.model.appointment.Appointment;
import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.AppointmentStatus;
import com.codecore.appointment.domain.valueobject.OrganizationId;
import com.codecore.appointment.domain.valueobject.PatientId;
import com.codecore.appointment.domain.valueobject.StaffAssignmentId;
import com.codecore.appointment.domain.valueobject.TenantId;
import com.codecore.appointment.infrastructure.persistence.mapper.AppointmentMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Hexagonal adapter: implements outbound Appointment persistence ports using R2DBC.
 * No child tables — Appointment is a single-row aggregate (ADR-014).
 */
@Repository
public class R2dbcAppointmentRepository implements AppointmentRepository, AppointmentQueryPort {

    private final SpringDataAppointmentRepository springDataAppointmentRepository;
    private final AppointmentMapper appointmentMapper;

    public R2dbcAppointmentRepository(
            SpringDataAppointmentRepository springDataAppointmentRepository,
            AppointmentMapper appointmentMapper
    ) {
        this.springDataAppointmentRepository = springDataAppointmentRepository;
        this.appointmentMapper = appointmentMapper;
    }

    @Override
    public Mono<Appointment> save(Appointment appointment) {
        return springDataAppointmentRepository
                .existsById(appointment.id().value())
                .flatMap(exists -> springDataAppointmentRepository.save(
                        appointmentMapper.toEntity(appointment, !exists)))
                .map(appointmentMapper::toDomain);
    }

    @Override
    public Mono<Appointment> findById(AppointmentId id) {
        return springDataAppointmentRepository.findById(id.value())
                .map(appointmentMapper::toDomain);
    }

    @Override
    public Mono<Appointment> findByIdAndTenantId(AppointmentId id, TenantId tenantId) {
        return springDataAppointmentRepository
                .findByAppointmentIdAndTenantId(id.value(), tenantId.value())
                .map(appointmentMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsById(AppointmentId id) {
        return springDataAppointmentRepository.existsById(id.value());
    }

    @Override
    public Mono<Boolean> existsByIdAndTenantId(AppointmentId id, TenantId tenantId) {
        return springDataAppointmentRepository.existsByAppointmentIdAndTenantId(
                id.value(),
                tenantId.value()
        );
    }

    @Override
    public Flux<Appointment> findByTenantId(TenantId tenantId) {
        return springDataAppointmentRepository.findAllByTenantId(tenantId.value())
                .map(appointmentMapper::toDomain);
    }

    @Override
    public Flux<Appointment> findByTenantIdAndStatus(TenantId tenantId, AppointmentStatus status) {
        return springDataAppointmentRepository
                .findAllByTenantIdAndStatus(tenantId.value(), status.name())
                .map(appointmentMapper::toDomain);
    }

    @Override
    public Flux<Appointment> findByTenantIdAndPatientId(TenantId tenantId, PatientId patientId) {
        return springDataAppointmentRepository
                .findAllByTenantIdAndPatientId(tenantId.value(), patientId.value())
                .map(appointmentMapper::toDomain);
    }

    @Override
    public Flux<Appointment> findByTenantIdAndOrganizationId(
            TenantId tenantId,
            OrganizationId organizationId
    ) {
        return springDataAppointmentRepository
                .findAllByTenantIdAndOrganizationId(tenantId.value(), organizationId.value())
                .map(appointmentMapper::toDomain);
    }

    @Override
    public Flux<Appointment> findByTenantIdAndStaffAssignmentId(
            TenantId tenantId,
            StaffAssignmentId staffAssignmentId
    ) {
        return springDataAppointmentRepository
                .findAllByTenantIdAndStaffAssignmentId(tenantId.value(), staffAssignmentId.value())
                .map(appointmentMapper::toDomain);
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId) {
        return springDataAppointmentRepository.countByTenantId(tenantId.value());
    }
}
