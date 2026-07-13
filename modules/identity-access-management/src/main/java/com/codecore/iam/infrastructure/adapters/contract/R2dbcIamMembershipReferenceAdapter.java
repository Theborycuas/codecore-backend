package com.codecore.iam.infrastructure.adapters.contract;

import com.codecore.iam.contract.reference.IamMembershipReferencePort;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class R2dbcIamMembershipReferenceAdapter implements IamMembershipReferencePort {

    private static final String ACTIVE = "ACTIVE";

    private final DatabaseClient databaseClient;

    public R2dbcIamMembershipReferenceAdapter(ConnectionFactory connectionFactory) {
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
                .map((row, metadata) -> Boolean.TRUE.equals(row.get(0, Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }
}
