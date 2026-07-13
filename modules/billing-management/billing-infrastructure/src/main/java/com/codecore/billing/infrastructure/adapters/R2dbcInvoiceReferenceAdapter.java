package com.codecore.billing.infrastructure.adapters;

import com.codecore.billing.contract.reference.InvoiceReferencePort;
import com.codecore.billing.domain.valueobject.InvoiceId;
import com.codecore.billing.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * In-process adapter for {@link InvoiceReferencePort} (ADR-013 / ADR-017).
 */
@Component
public class R2dbcInvoiceReferenceAdapter implements InvoiceReferencePort {

    private static final String ISSUED = "ISSUED";

    private final DatabaseClient databaseClient;

    public R2dbcInvoiceReferenceAdapter(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Boolean> existsIssuedByIdAndTenant(InvoiceId invoiceId, TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) > 0
                        FROM billing.invoice
                        WHERE invoice_id = :invoiceId
                          AND tenant_id = :tenantId
                          AND status = :status
                        """)
                .bind("invoiceId", invoiceId.value())
                .bind("tenantId", tenantId.value())
                .bind("status", ISSUED)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get(0, Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }
}
