package com.codecore.access.infrastructure.persistence.repository;

import com.codecore.access.application.dto.AdminInvitationView;
import com.codecore.access.application.port.out.InvitationAdminQueryRepository;
import com.codecore.access.application.query.InvitationListFilter;
import com.codecore.access.application.query.InvitationListQuery;
import com.codecore.access.application.query.PageQuery;
import com.codecore.access.application.query.PageQueryParser;
import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.domain.valueobject.InvitationStatus;
import com.codecore.access.domain.valueobject.MembershipId;
import com.codecore.access.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public class R2dbcInvitationAdminQueryRepository implements InvitationAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcInvitationAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminInvitationView> findByTenantId(
            TenantId tenantId,
            InvitationListQuery filter,
            PageQuery pageQuery
    ) {
        String orderColumn = PageQueryParser.invitationSqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";
        String statusClause = statusClause(filter.status());

        return databaseClient.sql("""
                        SELECT i.invitation_id, i.tenant_id, i.invited_email, i.invited_role_code,
                               i.invited_by_membership_id, i.expires_at, i.status, i.resulting_membership_id,
                               i.created_at, i.updated_at, i.accepted_at, i.revoked_at
                        FROM access.invitation i
                        WHERE i.tenant_id = :tenantId
                        %s
                        ORDER BY i.%s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(statusClause, orderColumn, direction))
                .bind("tenantId", tenantId.value())
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset())
                .map((row, metadata) -> toView(
                        row.get("invitation_id", UUID.class),
                        row.get("tenant_id", UUID.class),
                        row.get("invited_email", String.class),
                        row.get("invited_role_code", String.class),
                        row.get("invited_by_membership_id", UUID.class),
                        row.get("expires_at", Instant.class),
                        row.get("status", String.class),
                        row.get("resulting_membership_id", UUID.class),
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class),
                        row.get("accepted_at", Instant.class),
                        row.get("revoked_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId, InvitationListQuery filter) {
        String statusClause = statusClause(filter.status());
        return databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM access.invitation i
                        WHERE i.tenant_id = :tenantId
                        %s
                        """.formatted(statusClause))
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private static AdminInvitationView toView(
            UUID invitationId,
            UUID tenantId,
            String invitedEmail,
            String invitedRoleCode,
            UUID invitedByMembershipId,
            Instant expiresAt,
            String status,
            UUID resultingMembershipId,
            Instant createdAt,
            Instant updatedAt,
            Instant acceptedAt,
            Instant revokedAt
    ) {
        return new AdminInvitationView(
                new InvitationId(invitationId),
                new TenantId(tenantId),
                invitedEmail,
                invitedRoleCode,
                MembershipId.of(invitedByMembershipId),
                expiresAt,
                InvitationStatus.valueOf(status),
                resultingMembershipId == null ? null : MembershipId.of(resultingMembershipId),
                createdAt,
                updatedAt,
                acceptedAt,
                revokedAt
        );
    }

    private static String statusClause(InvitationListFilter filter) {
        return switch (filter) {
            case PENDING -> " AND i.status = 'PENDING' ";
            case ACCEPTED -> " AND i.status = 'ACCEPTED' ";
            case REVOKED -> " AND i.status = 'REVOKED' ";
            case EXPIRED -> " AND i.status = 'EXPIRED' ";
            case ALL -> "";
        };
    }
}
