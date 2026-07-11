package com.codecore.organization.infrastructure.adapters;

import com.codecore.organization.contract.reference.OrganizationReferencePort;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * In-process adapter for {@link OrganizationReferencePort} (ADR-013).
 */
@Component
public class R2dbcOrganizationReferenceAdapter implements OrganizationReferencePort {

    private static final String ACTIVE = "ACTIVE";

    private final DatabaseClient databaseClient;

    public R2dbcOrganizationReferenceAdapter(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Boolean> existsActiveByIdAndTenant(OrganizationId organizationId, TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) > 0
                        FROM org.organization
                        WHERE organization_id = :organizationId
                          AND tenant_id = :tenantId
                          AND status = :status
                        """)
                .bind("organizationId", organizationId.value())
                .bind("tenantId", tenantId.value())
                .bind("status", ACTIVE)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get(0, Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }
}
