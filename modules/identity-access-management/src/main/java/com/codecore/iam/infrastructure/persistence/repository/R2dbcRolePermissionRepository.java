package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.RolePermissionRepository;
import com.codecore.iam.domain.model.role.RolePermissionAssignment;
import com.codecore.iam.domain.valueobject.PermissionId;
import com.codecore.iam.domain.valueobject.RoleId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Hexagonal adapter: role-permission assignments via explicit SQL (composite PK).
 */
@Repository
public class R2dbcRolePermissionRepository implements RolePermissionRepository {

    private final DatabaseClient databaseClient;

    public R2dbcRolePermissionRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Void> assign(RoleId roleId, RolePermissionAssignment assignment) {
        return databaseClient.sql("""
                        INSERT INTO iam.role_permission (role_id, permission_id, assigned_at)
                        VALUES (:roleId, :permissionId, :assignedAt)
                        """)
                .bind("roleId", roleId.value())
                .bind("permissionId", assignment.permissionId().value())
                .bind("assignedAt", assignment.assignedAt())
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Mono<Void> revoke(RoleId roleId, PermissionId permissionId) {
        return databaseClient.sql("""
                        DELETE FROM iam.role_permission
                        WHERE role_id = :roleId AND permission_id = :permissionId
                        """)
                .bind("roleId", roleId.value())
                .bind("permissionId", permissionId.value())
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Flux<RolePermissionAssignment> findByRoleId(RoleId roleId) {
        return databaseClient.sql("""
                        SELECT permission_id, assigned_at
                        FROM iam.role_permission
                        WHERE role_id = :roleId
                        """)
                .bind("roleId", roleId.value())
                .map((row, metadata) -> new RolePermissionAssignment(
                        new PermissionId(row.get("permission_id", UUID.class)),
                        row.get("assigned_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Mono<Boolean> existsByRoleIdAndPermissionId(RoleId roleId, PermissionId permissionId) {
        return databaseClient.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM iam.role_permission
                            WHERE role_id = :roleId AND permission_id = :permissionId
                        )
                        """)
                .bind("roleId", roleId.value())
                .bind("permissionId", permissionId.value())
                .map((row, metadata) -> row.get(0, Boolean.class))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Void> replaceAll(RoleId roleId, Iterable<RolePermissionAssignment> assignments) {
        return databaseClient.sql("DELETE FROM iam.role_permission WHERE role_id = :roleId")
                .bind("roleId", roleId.value())
                .fetch()
                .rowsUpdated()
                .thenMany(Flux.fromIterable(assignments)
                        .flatMap(assignment -> assign(roleId, assignment)))
                .then();
    }
}
