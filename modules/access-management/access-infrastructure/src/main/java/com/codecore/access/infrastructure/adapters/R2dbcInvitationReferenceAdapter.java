package com.codecore.access.infrastructure.adapters;

import com.codecore.access.contract.reference.InvitationReferencePort;
import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * In-process adapter for {@link InvitationReferencePort} (ADR-013 / ADR-019).
 */
@Component
public class R2dbcInvitationReferenceAdapter implements InvitationReferencePort {

    private static final String PENDING = "PENDING";

    private final DatabaseClient databaseClient;

    public R2dbcInvitationReferenceAdapter(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Boolean> existsPendingByIdAndTenant(InvitationId invitationId, TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) > 0
                        FROM access.invitation
                        WHERE invitation_id = :invitationId
                          AND tenant_id = :tenantId
                          AND status = :status
                        """)
                .bind("invitationId", invitationId.value())
                .bind("tenantId", tenantId.value())
                .bind("status", PENDING)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get(0, Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }
}
