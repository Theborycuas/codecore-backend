package com.codecore.organization.infrastructure.adapters;

import com.codecore.organization.application.port.out.MembershipReferencePort;
import com.codecore.organization.domain.valueobject.MembershipId;
import com.codecore.organization.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class R2dbcMembershipReferenceAdapter implements MembershipReferencePort {

    private static final String ACTIVE = "ACTIVE";

    private final DatabaseClient databaseClient;

    public R2dbcMembershipReferenceAdapter(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Boolean> existsActiveByIdAndTenant(MembershipId membershipId, TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) > 0
                        FROM iam.identity_tenant_membership
                        WHERE membership_id = :membershipId
                          AND tenant_id = :tenantId
                          AND status = :status
                        """)
                .bind("membershipId", membershipId.value())
                .bind("tenantId", tenantId.value())
                .bind("status", ACTIVE)
                .map((row, metadata) -> row.get(0, Boolean.class))
                .one()
                .defaultIfEmpty(false);
    }
}
