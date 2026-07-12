package com.codecore.patient.infrastructure.adapters;

import com.codecore.patient.contract.reference.PatientReferencePort;
import com.codecore.patient.domain.valueobject.PatientId;
import com.codecore.patient.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * In-process adapter for {@link PatientReferencePort} (ADR-013).
 */
@Component
public class R2dbcPatientReferenceAdapter implements PatientReferencePort {

    private static final String ACTIVE = "ACTIVE";

    private final DatabaseClient databaseClient;

    public R2dbcPatientReferenceAdapter(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Boolean> existsActiveByIdAndTenant(PatientId patientId, TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) > 0
                        FROM clinical.patient
                        WHERE patient_id = :patientId
                          AND tenant_id = :tenantId
                          AND status = :status
                        """)
                .bind("patientId", patientId.value())
                .bind("tenantId", tenantId.value())
                .bind("status", ACTIVE)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get(0, Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }
}
