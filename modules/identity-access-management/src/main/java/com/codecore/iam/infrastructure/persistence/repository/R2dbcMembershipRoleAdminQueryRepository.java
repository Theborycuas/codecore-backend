package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.dto.AdminMembershipRoleView;
import com.codecore.iam.application.port.out.MembershipRoleAdminQueryRepository;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

@Repository
public class R2dbcMembershipRoleAdminQueryRepository implements MembershipRoleAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcMembershipRoleAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminMembershipRoleView> findByMembershipId(MembershipId membershipId, TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT r.role_id, r.code, r.name, r.status, r.system_role, mr.assigned_at
                        FROM iam.membership_role mr
                        INNER JOIN iam.role r ON r.role_id = mr.role_id
                        WHERE mr.membership_id = :membershipId
                          AND r.tenant_id = :tenantId
                        ORDER BY r.code ASC
                        """)
                .bind("membershipId", membershipId.value())
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> new AdminMembershipRoleView(
                        new RoleId(row.get("role_id", UUID.class)),
                        row.get("code", String.class),
                        row.get("name", String.class),
                        row.get("status", String.class),
                        row.get("system_role", Boolean.class),
                        row.get("assigned_at", Instant.class)
                ))
                .all();
    }
}
