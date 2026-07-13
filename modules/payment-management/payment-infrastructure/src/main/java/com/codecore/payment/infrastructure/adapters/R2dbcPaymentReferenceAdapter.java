package com.codecore.payment.infrastructure.adapters;

import com.codecore.payment.contract.reference.PaymentReferencePort;
import com.codecore.payment.domain.valueobject.PaymentId;
import com.codecore.payment.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * In-process adapter for {@link PaymentReferencePort} (ADR-013 / ADR-018 closeout 22.8).
 */
@Component
public class R2dbcPaymentReferenceAdapter implements PaymentReferencePort {

    private static final String RECORDED = "RECORDED";

    private final DatabaseClient databaseClient;

    public R2dbcPaymentReferenceAdapter(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Boolean> existsRecordedByIdAndTenant(PaymentId paymentId, TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) > 0
                        FROM payments.payment
                        WHERE payment_id = :paymentId
                          AND tenant_id = :tenantId
                          AND status = :status
                        """)
                .bind("paymentId", paymentId.value())
                .bind("tenantId", tenantId.value())
                .bind("status", RECORDED)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get(0, Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }
}
