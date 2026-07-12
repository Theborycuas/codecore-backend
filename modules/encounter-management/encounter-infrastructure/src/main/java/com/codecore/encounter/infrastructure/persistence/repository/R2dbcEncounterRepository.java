package com.codecore.encounter.infrastructure.persistence.repository;

import com.codecore.encounter.application.port.out.EncounterQueryPort;
import com.codecore.encounter.application.port.out.EncounterRepository;
import com.codecore.encounter.domain.model.encounter.Encounter;
import com.codecore.encounter.domain.valueobject.AppointmentId;
import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.EncounterStatus;
import com.codecore.encounter.domain.valueobject.OrganizationId;
import com.codecore.encounter.domain.valueobject.PatientId;
import com.codecore.encounter.domain.valueobject.StaffAssignmentId;
import com.codecore.encounter.domain.valueobject.TenantId;
import com.codecore.encounter.infrastructure.persistence.mapper.EncounterMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Hexagonal adapter: implements outbound Encounter persistence ports using R2DBC.
 * No child tables — Encounter is a single-row aggregate (ADR-015).
 */
@Repository
public class R2dbcEncounterRepository implements EncounterRepository, EncounterQueryPort {

    private final SpringDataEncounterRepository springDataEncounterRepository;
    private final EncounterMapper encounterMapper;

    public R2dbcEncounterRepository(
            SpringDataEncounterRepository springDataEncounterRepository,
            EncounterMapper encounterMapper
    ) {
        this.springDataEncounterRepository = springDataEncounterRepository;
        this.encounterMapper = encounterMapper;
    }

    @Override
    public Mono<Encounter> save(Encounter encounter) {
        return springDataEncounterRepository
                .existsById(encounter.id().value())
                .flatMap(exists -> springDataEncounterRepository.save(
                        encounterMapper.toEntity(encounter, !exists)))
                .map(encounterMapper::toDomain);
    }

    @Override
    public Mono<Encounter> findById(EncounterId id) {
        return springDataEncounterRepository.findById(id.value())
                .map(encounterMapper::toDomain);
    }

    @Override
    public Mono<Encounter> findByIdAndTenantId(EncounterId id, TenantId tenantId) {
        return springDataEncounterRepository
                .findByEncounterIdAndTenantId(id.value(), tenantId.value())
                .map(encounterMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsById(EncounterId id) {
        return springDataEncounterRepository.existsById(id.value());
    }

    @Override
    public Mono<Boolean> existsByIdAndTenantId(EncounterId id, TenantId tenantId) {
        return springDataEncounterRepository.existsByEncounterIdAndTenantId(
                id.value(),
                tenantId.value()
        );
    }

    @Override
    public Flux<Encounter> findByTenantId(TenantId tenantId) {
        return springDataEncounterRepository.findAllByTenantId(tenantId.value())
                .map(encounterMapper::toDomain);
    }

    @Override
    public Flux<Encounter> findByTenantIdAndStatus(TenantId tenantId, EncounterStatus status) {
        return springDataEncounterRepository
                .findAllByTenantIdAndStatus(tenantId.value(), status.name())
                .map(encounterMapper::toDomain);
    }

    @Override
    public Flux<Encounter> findByTenantIdAndPatientId(TenantId tenantId, PatientId patientId) {
        return springDataEncounterRepository
                .findAllByTenantIdAndPatientId(tenantId.value(), patientId.value())
                .map(encounterMapper::toDomain);
    }

    @Override
    public Flux<Encounter> findByTenantIdAndOrganizationId(
            TenantId tenantId,
            OrganizationId organizationId
    ) {
        return springDataEncounterRepository
                .findAllByTenantIdAndOrganizationId(tenantId.value(), organizationId.value())
                .map(encounterMapper::toDomain);
    }

    @Override
    public Flux<Encounter> findByTenantIdAndStaffAssignmentId(
            TenantId tenantId,
            StaffAssignmentId staffAssignmentId
    ) {
        return springDataEncounterRepository
                .findAllByTenantIdAndStaffAssignmentId(tenantId.value(), staffAssignmentId.value())
                .map(encounterMapper::toDomain);
    }

    @Override
    public Flux<Encounter> findByTenantIdAndAppointmentId(
            TenantId tenantId,
            AppointmentId appointmentId
    ) {
        return springDataEncounterRepository
                .findAllByTenantIdAndAppointmentId(tenantId.value(), appointmentId.value())
                .map(encounterMapper::toDomain);
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId) {
        return springDataEncounterRepository.countByTenantId(tenantId.value());
    }
}
