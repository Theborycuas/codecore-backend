package com.codecore.patient.infrastructure.persistence.repository;

import com.codecore.patient.infrastructure.persistence.entity.PatientEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataPatientRepository extends ReactiveCrudRepository<PatientEntity, UUID> {

    Mono<PatientEntity> findByPatientIdAndTenantId(UUID patientId, UUID tenantId);

    Mono<Boolean> existsByPatientIdAndTenantId(UUID patientId, UUID tenantId);

    Flux<PatientEntity> findAllByTenantId(UUID tenantId);

    Flux<PatientEntity> findAllByTenantIdAndStatus(UUID tenantId, String status);

    Flux<PatientEntity> findAllByTenantIdAndPrimaryOrganizationId(UUID tenantId, UUID primaryOrganizationId);

    Mono<Long> countByTenantId(UUID tenantId);
}
