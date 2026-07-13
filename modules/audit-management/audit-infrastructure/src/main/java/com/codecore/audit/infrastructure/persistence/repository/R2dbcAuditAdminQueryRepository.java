package com.codecore.audit.infrastructure.persistence.repository;

import com.codecore.audit.application.dto.AdminAuditView;
import com.codecore.audit.application.port.out.AuditAdminQueryRepository;
import com.codecore.audit.application.query.AuditListQuery;
import com.codecore.audit.application.query.PageQuery;
import com.codecore.audit.application.query.PageQueryParser;
import com.codecore.audit.domain.valueobject.ActionCode;
import com.codecore.audit.domain.valueobject.AuditEntryId;
import com.codecore.audit.domain.valueobject.AuditOutcome;
import com.codecore.audit.domain.valueobject.MembershipId;
import com.codecore.audit.domain.valueobject.ResourceId;
import com.codecore.audit.domain.valueobject.ResourceType;
import com.codecore.audit.domain.valueobject.TenantId;
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
public class R2dbcAuditAdminQueryRepository implements AuditAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcAuditAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminAuditView> findByTenantId(TenantId tenantId, AuditListQuery filter, PageQuery pageQuery) {
        String orderColumn = PageQueryParser.auditSqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";
        FilterSql filterSql = buildFilterSql(filter);

        return bindFilter(databaseClient.sql("""
                        SELECT a.audit_entry_id, a.tenant_id, a.occurred_at, a.action_code,
                               a.actor_membership_id, a.resource_type, a.resource_id, a.outcome, a.created_at
                        FROM audit.audit_entry a
                        WHERE a.tenant_id = :tenantId
                        %s
                        ORDER BY a.%s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(filterSql.clause(), orderColumn, direction)), filterSql)
                .bind("tenantId", tenantId.value())
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset())
                .map((row, metadata) -> toView(
                        row.get("audit_entry_id", UUID.class),
                        row.get("tenant_id", UUID.class),
                        row.get("occurred_at", Instant.class),
                        row.get("action_code", String.class),
                        row.get("actor_membership_id", UUID.class),
                        row.get("resource_type", String.class),
                        row.get("resource_id", UUID.class),
                        row.get("outcome", String.class),
                        row.get("created_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId, AuditListQuery filter) {
        FilterSql filterSql = buildFilterSql(filter);
        return bindFilter(databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM audit.audit_entry a
                        WHERE a.tenant_id = :tenantId
                        %s
                        """.formatted(filterSql.clause())), filterSql)
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private static AdminAuditView toView(
            UUID auditEntryId,
            UUID tenantId,
            Instant occurredAt,
            String actionCode,
            UUID actorMembershipId,
            String resourceType,
            UUID resourceId,
            String outcome,
            Instant createdAt
    ) {
        return new AdminAuditView(
                new AuditEntryId(auditEntryId),
                new TenantId(tenantId),
                occurredAt,
                ActionCode.of(actionCode),
                actorMembershipId == null ? null : MembershipId.of(actorMembershipId),
                ResourceType.of(resourceType),
                ResourceId.of(resourceId),
                AuditOutcome.valueOf(outcome),
                createdAt
        );
    }

    private static FilterSql buildFilterSql(AuditListQuery filter) {
        StringBuilder clause = new StringBuilder();
        Map<String, Object> binds = new HashMap<>();
        if (filter.actionCode() != null) {
            clause.append(" AND a.action_code = :actionCode ");
            binds.put("actionCode", filter.actionCode());
        }
        if (filter.resourceType() != null) {
            clause.append(" AND a.resource_type = :resourceType ");
            binds.put("resourceType", filter.resourceType());
        }
        if (filter.resourceId() != null) {
            clause.append(" AND a.resource_id = :resourceId ");
            binds.put("resourceId", filter.resourceId());
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

    private record FilterSql(String clause, Map<String, Object> binds) {
    }
}
