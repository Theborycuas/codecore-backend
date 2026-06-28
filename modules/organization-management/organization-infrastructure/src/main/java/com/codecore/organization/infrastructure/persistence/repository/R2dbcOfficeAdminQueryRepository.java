package com.codecore.organization.infrastructure.persistence.repository;

import com.codecore.organization.application.dto.AdminOfficeView;
import com.codecore.organization.application.port.out.OfficeAdminQueryRepository;
import com.codecore.organization.application.query.PageQuery;
import com.codecore.organization.application.query.PageQueryParser;
import com.codecore.organization.application.query.StructureListFilter;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OfficeStatus;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public class R2dbcOfficeAdminQueryRepository implements OfficeAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcOfficeAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminOfficeView> findByTenantId(
            TenantId tenantId,
            OrganizationId organizationId,
            StructureListFilter filter,
            PageQuery pageQuery
    ) {
        String orderColumn = PageQueryParser.officeSqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";
        String statusClause = statusClause(filter);

        return databaseClient.sql("""
                        SELECT office_id, tenant_id, organization_id, code, name, status, created_at, updated_at
                        FROM org.office
                        WHERE tenant_id = :tenantId
                          AND organization_id = :organizationId
                        %s
                        ORDER BY %s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(statusClause, orderColumn, direction))
                .bind("tenantId", tenantId.value())
                .bind("organizationId", organizationId.value())
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset())
                .map((row, metadata) -> new AdminOfficeView(
                        new OfficeId(row.get("office_id", UUID.class)),
                        new TenantId(row.get("tenant_id", UUID.class)),
                        new OrganizationId(row.get("organization_id", UUID.class)),
                        row.get("code", String.class),
                        row.get("name", String.class),
                        OfficeStatus.valueOf(row.get("status", String.class)),
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Mono<Long> countByTenantId(
            TenantId tenantId,
            OrganizationId organizationId,
            StructureListFilter filter
    ) {
        String statusClause = statusClause(filter);
        return databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM org.office
                        WHERE tenant_id = :tenantId
                          AND organization_id = :organizationId
                        %s
                        """.formatted(statusClause))
                .bind("tenantId", tenantId.value())
                .bind("organizationId", organizationId.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private static String statusClause(StructureListFilter filter) {
        return switch (filter) {
            case ACTIVE -> "AND status = 'ACTIVE'";
            case ARCHIVED -> "AND status = 'ARCHIVED'";
            case ALL -> "";
        };
    }
}
