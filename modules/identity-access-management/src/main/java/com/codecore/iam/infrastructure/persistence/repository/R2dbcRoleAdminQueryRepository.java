package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.dto.AdminRoleView;
import com.codecore.iam.application.port.out.RoleAdminQueryRepository;
import com.codecore.iam.application.query.PageQuery;
import com.codecore.iam.application.query.PageQueryParser;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.RoleStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public class R2dbcRoleAdminQueryRepository implements RoleAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcRoleAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminRoleView> findByTenantId(TenantId tenantId, PageQuery pageQuery) {
        String orderColumn = PageQueryParser.roleSqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";

        return databaseClient.sql("""
                        SELECT role_id, tenant_id, code, name, status, system_role, created_at, updated_at
                        FROM iam.role
                        WHERE tenant_id = :tenantId
                        ORDER BY %s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(orderColumn, direction))
                .bind("tenantId", tenantId.value())
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset())
                .map((row, metadata) -> new AdminRoleView(
                        new RoleId(row.get("role_id", UUID.class)),
                        new TenantId(row.get("tenant_id", UUID.class)),
                        row.get("code", String.class),
                        row.get("name", String.class),
                        RoleStatus.valueOf(row.get("status", String.class)),
                        row.get("system_role", Boolean.class),
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM iam.role
                        WHERE tenant_id = :tenantId
                        """)
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }
}
