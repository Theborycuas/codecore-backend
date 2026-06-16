package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.domain.model.membership.MembershipRoleAssignment;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.RoleId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Hexagonal adapter: membership-role assignments via explicit SQL (composite PK).
 */
@Repository
public class R2dbcMembershipRoleRepository implements MembershipRoleRepository {

    private final DatabaseClient databaseClient;

    public R2dbcMembershipRoleRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Void> assign(MembershipId membershipId, MembershipRoleAssignment assignment) {
        return databaseClient.sql("""
                        INSERT INTO iam.membership_role (membership_id, role_id, assigned_at)
                        VALUES (:membershipId, :roleId, :assignedAt)
                        """)
                .bind("membershipId", membershipId.value())
                .bind("roleId", assignment.roleId().value())
                .bind("assignedAt", assignment.assignedAt())
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Mono<Void> revoke(MembershipId membershipId, RoleId roleId) {
        return databaseClient.sql("""
                        DELETE FROM iam.membership_role
                        WHERE membership_id = :membershipId AND role_id = :roleId
                        """)
                .bind("membershipId", membershipId.value())
                .bind("roleId", roleId.value())
                .fetch()
                .rowsUpdated()
                .then();
    }

    @Override
    public Flux<MembershipRoleAssignment> findByMembershipId(MembershipId membershipId) {
        return databaseClient.sql("""
                        SELECT role_id, assigned_at
                        FROM iam.membership_role
                        WHERE membership_id = :membershipId
                        """)
                .bind("membershipId", membershipId.value())
                .map((row, metadata) -> new MembershipRoleAssignment(
                        new RoleId(row.get("role_id", UUID.class)),
                        row.get("assigned_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Mono<Boolean> existsByMembershipIdAndRoleId(MembershipId membershipId, RoleId roleId) {
        return databaseClient.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM iam.membership_role
                            WHERE membership_id = :membershipId AND role_id = :roleId
                        )
                        """)
                .bind("membershipId", membershipId.value())
                .bind("roleId", roleId.value())
                .map((row, metadata) -> row.get(0, Boolean.class))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Void> replaceAll(MembershipId membershipId, Iterable<MembershipRoleAssignment> assignments) {
        return databaseClient.sql("DELETE FROM iam.membership_role WHERE membership_id = :membershipId")
                .bind("membershipId", membershipId.value())
                .fetch()
                .rowsUpdated()
                .thenMany(Flux.fromIterable(assignments)
                        .flatMap(assignment -> assign(membershipId, assignment)))
                .then();
    }
}
