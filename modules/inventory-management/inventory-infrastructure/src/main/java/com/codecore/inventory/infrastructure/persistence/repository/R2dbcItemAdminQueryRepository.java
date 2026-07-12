package com.codecore.inventory.infrastructure.persistence.repository;

import com.codecore.inventory.application.dto.AdminItemView;
import com.codecore.inventory.application.port.out.ItemAdminQueryRepository;
import com.codecore.inventory.application.query.ItemListFilter;
import com.codecore.inventory.application.query.ItemListQuery;
import com.codecore.inventory.application.query.PageQuery;
import com.codecore.inventory.application.query.PageQueryParser;
import com.codecore.inventory.domain.valueobject.ItemId;
import com.codecore.inventory.domain.valueobject.ItemStatus;
import com.codecore.inventory.domain.valueobject.PrimaryOrganizationId;
import com.codecore.inventory.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
public class R2dbcItemAdminQueryRepository implements ItemAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcItemAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminItemView> findByTenantId(
            TenantId tenantId,
            ItemListQuery filter,
            PageQuery pageQuery
    ) {
        String orderColumn = PageQueryParser.itemSqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";
        FilterSql filterSql = buildFilterSql(filter);

        return bindFilter(databaseClient.sql("""
                        SELECT i.item_id, i.tenant_id, i.display_name, i.code,
                               i.primary_organization_id, i.status, i.created_at, i.updated_at
                        FROM inventory.item i
                        WHERE i.tenant_id = :tenantId
                        %s
                        ORDER BY i.%s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(filterSql.clause(), orderColumn, direction)), filterSql)
                .bind("tenantId", tenantId.value())
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset())
                .map((row, metadata) -> toView(
                        row.get("item_id", UUID.class),
                        row.get("tenant_id", UUID.class),
                        row.get("display_name", String.class),
                        row.get("code", String.class),
                        row.get("primary_organization_id", UUID.class),
                        row.get("status", String.class),
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId, ItemListQuery filter) {
        FilterSql filterSql = buildFilterSql(filter);
        return bindFilter(databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM inventory.item i
                        WHERE i.tenant_id = :tenantId
                        %s
                        """.formatted(filterSql.clause())), filterSql)
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private static AdminItemView toView(
            UUID itemId,
            UUID tenantId,
            String displayName,
            String code,
            UUID primaryOrganizationId,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new AdminItemView(
                new ItemId(itemId),
                new TenantId(tenantId),
                displayName,
                code,
                primaryOrganizationId == null ? null : PrimaryOrganizationId.of(primaryOrganizationId),
                ItemStatus.valueOf(status),
                createdAt,
                updatedAt
        );
    }

    private static FilterSql buildFilterSql(ItemListQuery filter) {
        StringBuilder clause = new StringBuilder();
        clause.append(statusClause(filter.status()));
        Map<String, Object> binds = new HashMap<>();
        if (filter.q() != null) {
            clause.append(" AND LOWER(i.display_name) LIKE LOWER(:q) ");
            binds.put("q", "%" + filter.q().trim() + "%");
        }
        if (filter.code() != null) {
            clause.append(" AND i.code = :code ");
            binds.put("code", filter.code().trim());
        }
        if (filter.primaryOrganizationId() != null) {
            clause.append(" AND i.primary_organization_id = :primaryOrganizationId ");
            binds.put("primaryOrganizationId", filter.primaryOrganizationId());
        }
        return new FilterSql(clause.toString(), binds);
    }

    private static DatabaseClient.GenericExecuteSpec bindFilter(
            DatabaseClient.GenericExecuteSpec spec,
            FilterSql filterSql
    ) {
        DatabaseClient.GenericExecuteSpec bound = spec;
        for (Map.Entry<String, Object> entry : filterSql.binds().entrySet()) {
            bound = bound.bind(entry.getKey(), entry.getValue());
        }
        return bound;
    }

    private static String statusClause(ItemListFilter filter) {
        return switch (filter) {
            case ACTIVE -> " AND i.status = 'ACTIVE' ";
            case ARCHIVED -> " AND i.status = 'ARCHIVED' ";
            case ALL -> "";
        };
    }

    private record FilterSql(String clause, Map<String, Object> binds) {
    }
}
