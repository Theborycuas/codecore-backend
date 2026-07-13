package com.codecore.audit.infrastructure.adapters;

import com.codecore.audit.contract.reference.AuditReferencePort;
import com.codecore.audit.domain.valueobject.AuditEntryId;
import com.codecore.audit.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * In-process adapter for {@link AuditReferencePort} (ADR-013 / ADR-020).
 */
@Component
public class R2dbcAuditReferenceAdapter implements AuditReferencePort {

    private final DatabaseClient databaseClient;

    public R2dbcAuditReferenceAdapter(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Boolean> existsByIdAndTenant(AuditEntryId auditEntryId, TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) > 0
                        FROM audit.audit_entry
                        WHERE audit_entry_id = :auditEntryId
                          AND tenant_id = :tenantId
                        """)
                .bind("auditEntryId", auditEntryId.value())
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> Boolean.TRUE.equals(row.get(0, Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }
}
