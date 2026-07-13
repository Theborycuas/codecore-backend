package com.codecore.iam.infrastructure.adapters.contract;

import com.codecore.iam.contract.reference.IamActiveMembershipByEmailPort;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class R2dbcIamActiveMembershipByEmailAdapter implements IamActiveMembershipByEmailPort {

    private static final String ACTIVE = "ACTIVE";

    private final DatabaseClient databaseClient;

    public R2dbcIamActiveMembershipByEmailAdapter(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Boolean> existsActiveByEmailAndTenant(EmailAddress email, TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) > 0
                        FROM iam.iam_user i
                        JOIN iam.identity_tenant_membership m ON m.identity_id = i.id
                        WHERE i.normalized_email = :email
                          AND m.tenant_id = :tenantId
                          AND m.status = :status
                        """)
                .bind("email", email.value())
                .bind("tenantId", tenantId.value())
                .bind("status", ACTIVE)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get(0, Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }
}
