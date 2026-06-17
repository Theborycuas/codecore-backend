package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.dto.AdminRolePermissionView;
import com.codecore.iam.application.port.out.RolePermissionAdminQueryRepository;
import com.codecore.iam.domain.valueobject.PermissionId;
import com.codecore.iam.domain.valueobject.RoleId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

@Repository
public class R2dbcRolePermissionAdminQueryRepository implements RolePermissionAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcRolePermissionAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminRolePermissionView> findByRoleId(RoleId roleId) {
        return databaseClient.sql("""
                        SELECT p.permission_id, p.code, p.description, rp.assigned_at
                        FROM iam.role_permission rp
                        INNER JOIN iam.permission p ON p.permission_id = rp.permission_id
                        WHERE rp.role_id = :roleId
                        ORDER BY p.code ASC
                        """)
                .bind("roleId", roleId.value())
                .map((row, metadata) -> new AdminRolePermissionView(
                        new PermissionId(row.get("permission_id", UUID.class)),
                        row.get("code", String.class),
                        row.get("description", String.class),
                        row.get("assigned_at", Instant.class)
                ))
                .all();
    }
}
