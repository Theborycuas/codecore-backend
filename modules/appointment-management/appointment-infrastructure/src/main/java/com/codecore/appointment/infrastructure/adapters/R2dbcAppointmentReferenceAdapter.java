package com.codecore.appointment.infrastructure.adapters;

import com.codecore.appointment.contract.reference.AppointmentReferencePort;
import com.codecore.appointment.contract.reference.AppointmentReferenceView;
import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.AppointmentStatus;
import com.codecore.appointment.domain.valueobject.PatientId;
import com.codecore.appointment.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * In-process adapter for {@link AppointmentReferencePort} (ADR-013 / ADR-015).
 */
@Component
public class R2dbcAppointmentReferenceAdapter implements AppointmentReferencePort {

    private static final String SCHEDULED = AppointmentStatus.SCHEDULED.name();
    private static final String COMPLETED = AppointmentStatus.COMPLETED.name();

    private final DatabaseClient databaseClient;

    public R2dbcAppointmentReferenceAdapter(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Boolean> existsScheduledByIdAndTenant(AppointmentId appointmentId, TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) > 0
                        FROM scheduling.appointment
                        WHERE appointment_id = :appointmentId
                          AND tenant_id = :tenantId
                          AND status = :status
                        """)
                .bind("appointmentId", appointmentId.value())
                .bind("tenantId", tenantId.value())
                .bind("status", SCHEDULED)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get(0, Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Optional<AppointmentReferenceView>> findLinkableByIdAndTenant(
            AppointmentId appointmentId,
            TenantId tenantId
    ) {
        return databaseClient.sql("""
                        SELECT appointment_id, patient_id, status
                        FROM scheduling.appointment
                        WHERE appointment_id = :appointmentId
                          AND tenant_id = :tenantId
                          AND status IN (:scheduled, :completed)
                        """)
                .bind("appointmentId", appointmentId.value())
                .bind("tenantId", tenantId.value())
                .bind("scheduled", SCHEDULED)
                .bind("completed", COMPLETED)
                .map((row, metadata) -> new AppointmentReferenceView(
                        new AppointmentId(row.get("appointment_id", UUID.class)),
                        PatientId.of(row.get("patient_id", UUID.class)),
                        AppointmentStatus.valueOf(row.get("status", String.class))
                ))
                .one()
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty());
    }
}
