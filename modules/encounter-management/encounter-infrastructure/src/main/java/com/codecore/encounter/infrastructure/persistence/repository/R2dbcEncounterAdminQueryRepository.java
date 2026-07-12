package com.codecore.encounter.infrastructure.persistence.repository;

import com.codecore.encounter.application.dto.AdminEncounterView;
import com.codecore.encounter.application.port.out.EncounterAdminQueryRepository;
import com.codecore.encounter.application.query.EncounterListFilter;
import com.codecore.encounter.application.query.EncounterListQuery;
import com.codecore.encounter.application.query.PageQuery;
import com.codecore.encounter.application.query.PageQueryParser;
import com.codecore.encounter.domain.valueobject.AppointmentId;
import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.EncounterStatus;
import com.codecore.encounter.domain.valueobject.OfficeId;
import com.codecore.encounter.domain.valueobject.OrganizationId;
import com.codecore.encounter.domain.valueobject.PatientId;
import com.codecore.encounter.domain.valueobject.StaffAssignmentId;
import com.codecore.encounter.domain.valueobject.TenantId;
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
public class R2dbcEncounterAdminQueryRepository implements EncounterAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcEncounterAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminEncounterView> findByTenantId(
            TenantId tenantId,
            EncounterListQuery filter,
            PageQuery pageQuery
    ) {
        String orderColumn = PageQueryParser.encounterSqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";
        FilterSql filterSql = buildFilterSql(filter);

        return bindFilter(databaseClient.sql("""
                        SELECT e.encounter_id, e.tenant_id, e.patient_id, e.staff_assignment_id,
                               e.organization_id, e.office_id, e.appointment_id,
                               e.started_at, e.ended_at, e.status,
                               e.created_at, e.updated_at
                        FROM records.encounter e
                        WHERE e.tenant_id = :tenantId
                        %s
                        ORDER BY e.%s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(filterSql.clause(), orderColumn, direction)), filterSql)
                .bind("tenantId", tenantId.value())
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset())
                .map((row, metadata) -> toView(
                        row.get("encounter_id", UUID.class),
                        row.get("tenant_id", UUID.class),
                        row.get("patient_id", UUID.class),
                        row.get("staff_assignment_id", UUID.class),
                        row.get("organization_id", UUID.class),
                        row.get("office_id", UUID.class),
                        row.get("appointment_id", UUID.class),
                        row.get("started_at", Instant.class),
                        row.get("ended_at", Instant.class),
                        row.get("status", String.class),
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId, EncounterListQuery filter) {
        FilterSql filterSql = buildFilterSql(filter);
        return bindFilter(databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM records.encounter e
                        WHERE e.tenant_id = :tenantId
                        %s
                        """.formatted(filterSql.clause())), filterSql)
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private static AdminEncounterView toView(
            UUID encounterId,
            UUID tenantId,
            UUID patientId,
            UUID staffAssignmentId,
            UUID organizationId,
            UUID officeId,
            UUID appointmentId,
            Instant startedAt,
            Instant endedAt,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new AdminEncounterView(
                new EncounterId(encounterId),
                new TenantId(tenantId),
                PatientId.of(patientId),
                StaffAssignmentId.of(staffAssignmentId),
                OrganizationId.of(organizationId),
                officeId == null ? null : OfficeId.of(officeId),
                appointmentId == null ? null : AppointmentId.of(appointmentId),
                startedAt,
                endedAt,
                EncounterStatus.valueOf(status),
                createdAt,
                updatedAt
        );
    }

    private static FilterSql buildFilterSql(EncounterListQuery filter) {
        StringBuilder clause = new StringBuilder();
        clause.append(statusClause(filter.status()));
        Map<String, Object> binds = new HashMap<>();
        if (filter.organizationId() != null) {
            clause.append(" AND e.organization_id = :organizationId ");
            binds.put("organizationId", filter.organizationId());
        }
        if (filter.patientId() != null) {
            clause.append(" AND e.patient_id = :patientId ");
            binds.put("patientId", filter.patientId());
        }
        if (filter.staffAssignmentId() != null) {
            clause.append(" AND e.staff_assignment_id = :staffAssignmentId ");
            binds.put("staffAssignmentId", filter.staffAssignmentId());
        }
        if (filter.officeId() != null) {
            clause.append(" AND e.office_id = :officeId ");
            binds.put("officeId", filter.officeId());
        }
        if (filter.appointmentId() != null) {
            clause.append(" AND e.appointment_id = :appointmentId ");
            binds.put("appointmentId", filter.appointmentId());
        }
        if (filter.from() != null) {
            clause.append(" AND e.started_at >= :from ");
            binds.put("from", filter.from());
        }
        if (filter.to() != null) {
            clause.append(" AND e.started_at < :to ");
            binds.put("to", filter.to());
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

    private static String statusClause(EncounterListFilter filter) {
        return switch (filter) {
            case IN_PROGRESS -> " AND e.status = 'IN_PROGRESS' ";
            case CANCELLED -> " AND e.status = 'CANCELLED' ";
            case COMPLETED -> " AND e.status = 'COMPLETED' ";
            case ALL -> "";
        };
    }

    private record FilterSql(String clause, Map<String, Object> binds) {
    }
}
