package com.codecore.organization.infrastructure.persistence.repository;

import com.codecore.organization.application.dto.AdminStaffAssignmentView;
import com.codecore.organization.application.port.out.StaffAssignmentAdminQueryRepository;
import com.codecore.organization.application.query.PageQuery;
import com.codecore.organization.application.query.PageQueryParser;
import com.codecore.organization.application.query.StaffAssignmentListFilter;
import com.codecore.organization.domain.valueobject.MembershipId;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import com.codecore.organization.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class R2dbcStaffAssignmentAdminQueryRepository implements StaffAssignmentAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcStaffAssignmentAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminStaffAssignmentView> findByTenantId(
            TenantId tenantId,
            StaffAssignmentListFilter filter,
            PageQuery pageQuery
    ) {
        FilterClause filterClause = buildFilterClause(filter);
        String orderColumn = PageQueryParser.staffAssignmentSqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql("""
                        SELECT assignment_id, tenant_id, membership_id, organization_id, office_id, created_at, updated_at
                        FROM org.staff_assignment
                        WHERE tenant_id = :tenantId
                        %s
                        ORDER BY %s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(filterClause.clause(), orderColumn, direction))
                .bind("tenantId", tenantId.value())
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset());

        spec = bindFilterParams(spec, filterClause);

        return spec.map((row, metadata) -> new AdminStaffAssignmentView(
                        new StaffAssignmentId(row.get("assignment_id", UUID.class)),
                        new TenantId(row.get("tenant_id", UUID.class)),
                        new MembershipId(row.get("membership_id", UUID.class)),
                        new OrganizationId(row.get("organization_id", UUID.class)),
                        row.get("office_id", UUID.class) != null
                                ? new OfficeId(row.get("office_id", UUID.class))
                                : null,
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId, StaffAssignmentListFilter filter) {
        FilterClause filterClause = buildFilterClause(filter);
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM org.staff_assignment
                        WHERE tenant_id = :tenantId
                        %s
                        """.formatted(filterClause.clause()))
                .bind("tenantId", tenantId.value());

        spec = bindFilterParams(spec, filterClause);

        return spec.map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private static FilterClause buildFilterClause(StaffAssignmentListFilter filter) {
        UUID membershipId = filter.membershipId().map(MembershipId::value).orElse(null);
        UUID organizationId = filter.organizationId().map(OrganizationId::value).orElse(null);
        UUID officeId = filter.officeId().map(OfficeId::value).orElse(null);

        List<String> clauses = new ArrayList<>();
        if (membershipId != null) {
            clauses.add("AND membership_id = :membershipId");
        }
        if (organizationId != null) {
            clauses.add("AND organization_id = :organizationId");
        }
        if (officeId != null) {
            clauses.add("AND office_id = :officeId");
        }
        return new FilterClause(String.join("\n  ", clauses), membershipId, organizationId, officeId);
    }

    private static DatabaseClient.GenericExecuteSpec bindFilterParams(
            DatabaseClient.GenericExecuteSpec spec,
            FilterClause filterClause
    ) {
        if (filterClause.membershipId() != null) {
            spec = spec.bind("membershipId", filterClause.membershipId());
        }
        if (filterClause.organizationId() != null) {
            spec = spec.bind("organizationId", filterClause.organizationId());
        }
        if (filterClause.officeId() != null) {
            spec = spec.bind("officeId", filterClause.officeId());
        }
        return spec;
    }

    private record FilterClause(String clause, UUID membershipId, UUID organizationId, UUID officeId) {
    }
}
