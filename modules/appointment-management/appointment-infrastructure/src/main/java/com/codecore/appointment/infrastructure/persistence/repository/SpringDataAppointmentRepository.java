package com.codecore.appointment.infrastructure.persistence.repository;

import com.codecore.appointment.infrastructure.persistence.entity.AppointmentEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataAppointmentRepository extends ReactiveCrudRepository<AppointmentEntity, UUID> {

    Mono<AppointmentEntity> findByAppointmentIdAndTenantId(UUID appointmentId, UUID tenantId);

    Mono<Boolean> existsByAppointmentIdAndTenantId(UUID appointmentId, UUID tenantId);

    Flux<AppointmentEntity> findAllByTenantId(UUID tenantId);

    Flux<AppointmentEntity> findAllByTenantIdAndStatus(UUID tenantId, String status);

    Flux<AppointmentEntity> findAllByTenantIdAndPatientId(UUID tenantId, UUID patientId);

    Flux<AppointmentEntity> findAllByTenantIdAndOrganizationId(UUID tenantId, UUID organizationId);

    Flux<AppointmentEntity> findAllByTenantIdAndStaffAssignmentId(UUID tenantId, UUID staffAssignmentId);

    Mono<Long> countByTenantId(UUID tenantId);
}
