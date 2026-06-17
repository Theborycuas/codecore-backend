package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.dto.AdminMembershipView;
import com.codecore.iam.application.port.out.MembershipAdminQueryRepository;
import com.codecore.iam.application.query.PageQuery;
import com.codecore.iam.application.query.PageQueryParser;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public class R2dbcMembershipAdminQueryRepository implements MembershipAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcMembershipAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminMembershipView> findByTenantId(TenantId tenantId, PageQuery pageQuery) {
        String orderColumn = PageQueryParser.membershipSqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";

        return databaseClient.sql("""
                        SELECT m.membership_id, m.identity_id, m.tenant_id, m.status,
                               m.created_at, m.updated_at, u.normalized_email
                        FROM iam.identity_tenant_membership m
                        INNER JOIN iam.iam_user u ON u.id = m.identity_id
                        WHERE m.tenant_id = :tenantId
                        ORDER BY %s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(orderColumn, direction))
                .bind("tenantId", tenantId.value())
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset())
                .map((row, metadata) -> new AdminMembershipView(
                        new MembershipId(row.get("membership_id", UUID.class)),
                        new IdentityId(row.get("identity_id", UUID.class)),
                        new TenantId(row.get("tenant_id", UUID.class)),
                        MembershipStatus.valueOf(row.get("status", String.class)),
                        row.get("normalized_email", String.class),
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM iam.identity_tenant_membership m
                        WHERE m.tenant_id = :tenantId
                        """)
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }
}
