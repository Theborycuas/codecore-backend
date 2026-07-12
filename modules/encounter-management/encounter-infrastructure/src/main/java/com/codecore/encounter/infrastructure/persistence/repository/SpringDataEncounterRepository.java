package com.codecore.encounter.infrastructure.persistence.repository;

import com.codecore.encounter.infrastructure.persistence.entity.EncounterEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataEncounterRepository extends ReactiveCrudRepository<EncounterEntity, UUID> {

    Mono<EncounterEntity> findByEncounterIdAndTenantId(UUID encounterId, UUID tenantId);

    Mono<Boolean> existsByEncounterIdAndTenantId(UUID encounterId, UUID tenantId);

    Flux<EncounterEntity> findAllByTenantId(UUID tenantId);

    Flux<EncounterEntity> findAllByTenantIdAndStatus(UUID tenantId, String status);

    Flux<EncounterEntity> findAllByTenantIdAndPatientId(UUID tenantId, UUID patientId);

    Flux<EncounterEntity> findAllByTenantIdAndOrganizationId(UUID tenantId, UUID organizationId);

    Flux<EncounterEntity> findAllByTenantIdAndStaffAssignmentId(UUID tenantId, UUID staffAssignmentId);

    Flux<EncounterEntity> findAllByTenantIdAndAppointmentId(UUID tenantId, UUID appointmentId);

    Mono<Long> countByTenantId(UUID tenantId);
}
