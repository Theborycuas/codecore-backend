package com.codecore.patient.infrastructure.persistence.repository;

import com.codecore.patient.application.port.out.PatientQueryPort;
import com.codecore.patient.application.port.out.PatientRepository;
import com.codecore.patient.domain.model.patient.Patient;
import com.codecore.patient.domain.valueobject.ExternalIdentifier;
import com.codecore.patient.domain.valueobject.ExternalIdentifiers;
import com.codecore.patient.domain.valueobject.PatientId;
import com.codecore.patient.domain.valueobject.PatientStatus;
import com.codecore.patient.domain.valueobject.PrimaryOrganizationId;
import com.codecore.patient.domain.valueobject.TenantId;
import com.codecore.patient.infrastructure.persistence.entity.PatientEntity;
import com.codecore.patient.infrastructure.persistence.mapper.PatientMapper;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Hexagonal adapter: implements outbound Patient persistence ports using R2DBC.
 * External identifiers are owned by the Patient aggregate (same BC) and stored in a child table.
 */
@Repository
public class R2dbcPatientRepository implements PatientRepository, PatientQueryPort {

    private final SpringDataPatientRepository springDataPatientRepository;
    private final PatientMapper patientMapper;
    private final DatabaseClient databaseClient;

    public R2dbcPatientRepository(
            SpringDataPatientRepository springDataPatientRepository,
            PatientMapper patientMapper,
            ConnectionFactory connectionFactory
    ) {
        this.springDataPatientRepository = springDataPatientRepository;
        this.patientMapper = patientMapper;
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Patient> save(Patient patient) {
        return springDataPatientRepository
                .existsById(patient.id().value())
                .flatMap(exists -> springDataPatientRepository.save(
                        patientMapper.toEntity(patient, !exists)))
                .flatMap(entity -> replaceExternalIdentifiers(entity, patient.externalIdentifiers())
                        .then(toDomain(entity)));
    }

    @Override
    public Mono<Patient> findById(PatientId id) {
        return springDataPatientRepository.findById(id.value())
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<Patient> findByIdAndTenantId(PatientId id, TenantId tenantId) {
        return springDataPatientRepository.findByPatientIdAndTenantId(id.value(), tenantId.value())
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsById(PatientId id) {
        return springDataPatientRepository.existsById(id.value());
    }

    @Override
    public Mono<Boolean> existsByIdAndTenantId(PatientId id, TenantId tenantId) {
        return springDataPatientRepository.existsByPatientIdAndTenantId(id.value(), tenantId.value());
    }

    @Override
    public Flux<Patient> findByTenantId(TenantId tenantId) {
        return springDataPatientRepository.findAllByTenantId(tenantId.value())
                .flatMap(this::toDomain);
    }

    @Override
    public Flux<Patient> findByTenantIdAndStatus(TenantId tenantId, PatientStatus status) {
        return springDataPatientRepository.findAllByTenantIdAndStatus(tenantId.value(), status.name())
                .flatMap(this::toDomain);
    }

    @Override
    public Flux<Patient> findByTenantIdAndPrimaryOrganizationId(
            TenantId tenantId,
            PrimaryOrganizationId primaryOrganizationId
    ) {
        return springDataPatientRepository.findAllByTenantIdAndPrimaryOrganizationId(
                        tenantId.value(),
                        primaryOrganizationId.value()
                )
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId) {
        return springDataPatientRepository.countByTenantId(tenantId.value());
    }

    private Mono<Patient> toDomain(PatientEntity entity) {
        return loadExternalIdentifiers(entity.getPatientId())
                .map(identifiers -> patientMapper.toDomain(entity, identifiers));
    }

    private Mono<ExternalIdentifiers> loadExternalIdentifiers(UUID patientId) {
        return databaseClient.sql("""
                        SELECT identifier_type, identifier_value
                        FROM clinical.patient_external_identifier
                        WHERE patient_id = :patientId
                        ORDER BY identifier_type
                        """)
                .bind("patientId", patientId)
                .map((row, metadata) -> ExternalIdentifier.of(
                        row.get("identifier_type", String.class),
                        row.get("identifier_value", String.class)
                ))
                .all()
                .collectList()
                .map(ExternalIdentifiers::of);
    }

    private Mono<Void> replaceExternalIdentifiers(PatientEntity entity, ExternalIdentifiers identifiers) {
        return databaseClient.sql("""
                        DELETE FROM clinical.patient_external_identifier
                        WHERE patient_id = :patientId
                        """)
                .bind("patientId", entity.getPatientId())
                .fetch()
                .rowsUpdated()
                .thenMany(Flux.fromIterable(identifiers.asSet())
                        .concatMap(identifier -> databaseClient.sql("""
                                        INSERT INTO clinical.patient_external_identifier
                                            (patient_id, tenant_id, identifier_type, identifier_value)
                                        VALUES
                                            (:patientId, :tenantId, :identifierType, :identifierValue)
                                        """)
                                .bind("patientId", entity.getPatientId())
                                .bind("tenantId", entity.getTenantId())
                                .bind("identifierType", identifier.type().value())
                                .bind("identifierValue", identifier.value())
                                .fetch()
                                .rowsUpdated()))
                .then();
    }
}
