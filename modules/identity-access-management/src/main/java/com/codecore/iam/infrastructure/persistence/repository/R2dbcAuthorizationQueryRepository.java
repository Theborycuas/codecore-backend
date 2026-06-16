package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.AuthorizationQueryRepository;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleStatus;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Hexagonal adapter: authorization graph lookups via explicit SQL joins.
 */
@Repository
public class R2dbcAuthorizationQueryRepository implements AuthorizationQueryRepository {

    private static final String ACTIVE_ROLE = RoleStatus.ACTIVE.name();

    private final DatabaseClient databaseClient;

    public R2dbcAuthorizationQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Boolean> existsPermissionForMembership(MembershipId membershipId, PermissionCode permissionCode) {
        return databaseClient.sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM iam.membership_role mr
                            INNER JOIN iam.role r ON r.role_id = mr.role_id
                            INNER JOIN iam.role_permission rp ON rp.role_id = mr.role_id
                            INNER JOIN iam.permission p ON p.permission_id = rp.permission_id
                            WHERE mr.membership_id = :membershipId
                              AND p.code = :permissionCode
                              AND r.status = :activeRole
                        )
                        """)
                .bind("membershipId", membershipId.value())
                .bind("permissionCode", permissionCode.value())
                .bind("activeRole", ACTIVE_ROLE)
                .map((row, metadata) -> row.get(0, Boolean.class))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> existsAnyPermissionForMembership(
            MembershipId membershipId,
            Collection<String> permissionCodes
    ) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return Mono.just(false);
        }
        return databaseClient.sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM iam.membership_role mr
                            INNER JOIN iam.role r ON r.role_id = mr.role_id
                            INNER JOIN iam.role_permission rp ON rp.role_id = mr.role_id
                            INNER JOIN iam.permission p ON p.permission_id = rp.permission_id
                            WHERE mr.membership_id = :membershipId
                              AND p.code = ANY(:permissionCodes)
                              AND r.status = :activeRole
                        )
                        """)
                .bind("membershipId", membershipId.value())
                .bind("permissionCodes", permissionCodes.toArray(String[]::new))
                .bind("activeRole", ACTIVE_ROLE)
                .map((row, metadata) -> row.get(0, Boolean.class))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> existsRoleForMembership(MembershipId membershipId, RoleCode roleCode) {
        return databaseClient.sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM iam.membership_role mr
                            INNER JOIN iam.role r ON r.role_id = mr.role_id
                            WHERE mr.membership_id = :membershipId
                              AND r.code = :roleCode
                              AND r.status = :activeRole
                        )
                        """)
                .bind("membershipId", membershipId.value())
                .bind("roleCode", roleCode.value())
                .bind("activeRole", ACTIVE_ROLE)
                .map((row, metadata) -> row.get(0, Boolean.class))
                .one()
                .defaultIfEmpty(false);
    }
}
