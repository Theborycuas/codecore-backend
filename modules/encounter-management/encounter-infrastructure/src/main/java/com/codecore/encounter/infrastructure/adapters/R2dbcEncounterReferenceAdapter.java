package com.codecore.encounter.infrastructure.adapters;

import com.codecore.encounter.contract.reference.EncounterReferencePort;
import com.codecore.encounter.contract.reference.EncounterReferenceView;
import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.EncounterStatus;
import com.codecore.encounter.domain.valueobject.PatientId;
import com.codecore.encounter.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * In-process adapter for {@link EncounterReferencePort} (ADR-013 / ADR-015).
 */
@Component
public class R2dbcEncounterReferenceAdapter implements EncounterReferencePort {

    private static final String IN_PROGRESS = EncounterStatus.IN_PROGRESS.name();
    private static final String COMPLETED = EncounterStatus.COMPLETED.name();

    private final DatabaseClient databaseClient;

    public R2dbcEncounterReferenceAdapter(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Boolean> existsInProgressByIdAndTenant(EncounterId encounterId, TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) > 0
                        FROM records.encounter
                        WHERE encounter_id = :encounterId
                          AND tenant_id = :tenantId
                          AND status = :status
                        """)
                .bind("encounterId", encounterId.value())
                .bind("tenantId", tenantId.value())
                .bind("status", IN_PROGRESS)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get(0, Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Optional<EncounterReferenceView>> findLinkableByIdAndTenant(
            EncounterId encounterId,
            TenantId tenantId
    ) {
        return databaseClient.sql("""
                        SELECT encounter_id, patient_id, status
                        FROM records.encounter
                        WHERE encounter_id = :encounterId
                          AND tenant_id = :tenantId
                          AND status IN (:inProgress, :completed)
                        """)
                .bind("encounterId", encounterId.value())
                .bind("tenantId", tenantId.value())
                .bind("inProgress", IN_PROGRESS)
                .bind("completed", COMPLETED)
                .map((row, metadata) -> new EncounterReferenceView(
                        new EncounterId(row.get("encounter_id", UUID.class)),
                        PatientId.of(row.get("patient_id", UUID.class)),
                        EncounterStatus.valueOf(row.get("status", String.class))
                ))
                .one()
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
    }
}
