package com.codecore.appointment.infrastructure.persistence.repository;

import com.codecore.appointment.application.dto.AdminAppointmentView;
import com.codecore.appointment.application.port.out.AppointmentAdminQueryRepository;
import com.codecore.appointment.application.query.AppointmentListFilter;
import com.codecore.appointment.application.query.AppointmentListQuery;
import com.codecore.appointment.application.query.PageQuery;
import com.codecore.appointment.application.query.PageQueryParser;
import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.AppointmentStatus;
import com.codecore.appointment.domain.valueobject.OfficeId;
import com.codecore.appointment.domain.valueobject.OrganizationId;
import com.codecore.appointment.domain.valueobject.PatientId;
import com.codecore.appointment.domain.valueobject.StaffAssignmentId;
import com.codecore.appointment.domain.valueobject.TenantId;
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
public class R2dbcAppointmentAdminQueryRepository implements AppointmentAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcAppointmentAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminAppointmentView> findByTenantId(
            TenantId tenantId,
            AppointmentListQuery filter,
            PageQuery pageQuery
    ) {
        String orderColumn = PageQueryParser.appointmentSqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";
        FilterSql filterSql = buildFilterSql(filter);

        return bindFilter(databaseClient.sql("""
                        SELECT a.appointment_id, a.tenant_id, a.patient_id, a.staff_assignment_id,
                               a.organization_id, a.office_id, a.starts_at, a.ends_at, a.status,
                               a.created_at, a.updated_at
                        FROM scheduling.appointment a
                        WHERE a.tenant_id = :tenantId
                        %s
                        ORDER BY a.%s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(filterSql.clause(), orderColumn, direction)), filterSql)
                .bind("tenantId", tenantId.value())
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset())
                .map((row, metadata) -> toView(
                        row.get("appointment_id", UUID.class),
                        row.get("tenant_id", UUID.class),
                        row.get("patient_id", UUID.class),
                        row.get("staff_assignment_id", UUID.class),
                        row.get("organization_id", UUID.class),
                        row.get("office_id", UUID.class),
                        row.get("starts_at", Instant.class),
                        row.get("ends_at", Instant.class),
                        row.get("status", String.class),
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId, AppointmentListQuery filter) {
        FilterSql filterSql = buildFilterSql(filter);
        return bindFilter(databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM scheduling.appointment a
                        WHERE a.tenant_id = :tenantId
                        %s
                        """.formatted(filterSql.clause())), filterSql)
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private static AdminAppointmentView toView(
            UUID appointmentId,
            UUID tenantId,
            UUID patientId,
            UUID staffAssignmentId,
            UUID organizationId,
            UUID officeId,
            Instant startsAt,
            Instant endsAt,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new AdminAppointmentView(
                new AppointmentId(appointmentId),
                new TenantId(tenantId),
                PatientId.of(patientId),
                StaffAssignmentId.of(staffAssignmentId),
                OrganizationId.of(organizationId),
                officeId == null ? null : OfficeId.of(officeId),
                startsAt,
                endsAt,
                AppointmentStatus.valueOf(status),
                createdAt,
                updatedAt
        );
    }

    private static FilterSql buildFilterSql(AppointmentListQuery filter) {
        StringBuilder clause = new StringBuilder();
        clause.append(statusClause(filter.status()));
        Map<String, Object> binds = new HashMap<>();
        if (filter.organizationId() != null) {
            clause.append(" AND a.organization_id = :organizationId ");
            binds.put("organizationId", filter.organizationId());
        }
        if (filter.patientId() != null) {
            clause.append(" AND a.patient_id = :patientId ");
            binds.put("patientId", filter.patientId());
        }
        if (filter.staffAssignmentId() != null) {
            clause.append(" AND a.staff_assignment_id = :staffAssignmentId ");
            binds.put("staffAssignmentId", filter.staffAssignmentId());
        }
        if (filter.officeId() != null) {
            clause.append(" AND a.office_id = :officeId ");
            binds.put("officeId", filter.officeId());
        }
        if (filter.from() != null) {
            clause.append(" AND a.starts_at >= :from ");
            binds.put("from", filter.from());
        }
        if (filter.to() != null) {
            clause.append(" AND a.starts_at < :to ");
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

    private static String statusClause(AppointmentListFilter filter) {
        return switch (filter) {
            case SCHEDULED -> " AND a.status = 'SCHEDULED' ";
            case CANCELLED -> " AND a.status = 'CANCELLED' ";
            case COMPLETED -> " AND a.status = 'COMPLETED' ";
            case ALL -> "";
        };
    }

    private record FilterSql(String clause, Map<String, Object> binds) {
    }
}
