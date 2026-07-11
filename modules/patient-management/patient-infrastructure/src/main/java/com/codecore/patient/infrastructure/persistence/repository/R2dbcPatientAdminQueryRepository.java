package com.codecore.patient.infrastructure.persistence.repository;

import com.codecore.patient.application.dto.AdminPatientView;
import com.codecore.patient.application.port.out.PatientAdminQueryRepository;
import com.codecore.patient.application.query.PageQuery;
import com.codecore.patient.application.query.PageQueryParser;
import com.codecore.patient.application.query.PatientListFilter;
import com.codecore.patient.application.query.PatientListQuery;
import com.codecore.patient.domain.valueobject.PatientId;
import com.codecore.patient.domain.valueobject.PatientStatus;
import com.codecore.patient.domain.valueobject.PrimaryOrganizationId;
import com.codecore.patient.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class R2dbcPatientAdminQueryRepository implements PatientAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcPatientAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminPatientView> findByTenantId(
            TenantId tenantId,
            PatientListQuery filter,
            PageQuery pageQuery
    ) {
        String orderColumn = PageQueryParser.patientSqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";
        FilterSql filterSql = buildFilterSql(filter);

        return bindFilter(databaseClient.sql("""
                        SELECT p.patient_id, p.tenant_id, p.display_name, p.contact_email, p.contact_phone,
                               p.date_of_birth, p.primary_organization_id, p.status, p.created_at, p.updated_at
                        FROM clinical.patient p
                        WHERE p.tenant_id = :tenantId
                        %s
                        ORDER BY p.%s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(filterSql.clause(), orderColumn, direction)), filterSql)
                .bind("tenantId", tenantId.value())
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset())
                .map((row, metadata) -> new PatientRow(
                        row.get("patient_id", UUID.class),
                        row.get("tenant_id", UUID.class),
                        row.get("display_name", String.class),
                        row.get("contact_email", String.class),
                        row.get("contact_phone", String.class),
                        row.get("date_of_birth", LocalDate.class),
                        row.get("primary_organization_id", UUID.class),
                        row.get("status", String.class),
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class)
                ))
                .all()
                .collectList()
                .flatMapMany(this::enrichWithExternalIdentifiers);
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId, PatientListQuery filter) {
        FilterSql filterSql = buildFilterSql(filter);
        return bindFilter(databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM clinical.patient p
                        WHERE p.tenant_id = :tenantId
                        %s
                        """.formatted(filterSql.clause())), filterSql)
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private Flux<AdminPatientView> enrichWithExternalIdentifiers(List<PatientRow> rows) {
        if (rows.isEmpty()) {
            return Flux.empty();
        }
        List<UUID> ids = rows.stream().map(PatientRow::patientId).toList();
        String inClause = ids.stream().map(id -> "'" + id + "'").collect(Collectors.joining(", "));
        return databaseClient.sql("""
                        SELECT patient_id, identifier_type, identifier_value
                        FROM clinical.patient_external_identifier
                        WHERE patient_id IN (%s)
                        ORDER BY identifier_type
                        """.formatted(inClause))
                .map((row, metadata) -> Map.entry(
                        row.get("patient_id", UUID.class),
                        new AdminPatientView.ExternalIdentifierItem(
                                row.get("identifier_type", String.class),
                                row.get("identifier_value", String.class)
                        )
                ))
                .all()
                .collectList()
                .flatMapMany(entries -> {
                    Map<UUID, List<AdminPatientView.ExternalIdentifierItem>> byPatient = new HashMap<>();
                    for (Map.Entry<UUID, AdminPatientView.ExternalIdentifierItem> entry : entries) {
                        byPatient.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).add(entry.getValue());
                    }
                    return Flux.fromIterable(rows.stream()
                            .map(row -> toView(row, byPatient.getOrDefault(row.patientId(), List.of())))
                            .toList());
                });
    }

    private static AdminPatientView toView(
            PatientRow row,
            List<AdminPatientView.ExternalIdentifierItem> identifiers
    ) {
        return new AdminPatientView(
                new PatientId(row.patientId()),
                new TenantId(row.tenantId()),
                row.displayName(),
                row.contactEmail(),
                row.contactPhone(),
                row.dateOfBirth(),
                row.primaryOrganizationId() == null ? null : PrimaryOrganizationId.of(row.primaryOrganizationId()),
                identifiers,
                PatientStatus.valueOf(row.status()),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private static FilterSql buildFilterSql(PatientListQuery filter) {
        StringBuilder clause = new StringBuilder();
        clause.append(statusClause(filter.status()));
        Map<String, Object> binds = new HashMap<>();
        if (filter.q() != null) {
            clause.append(" AND LOWER(p.display_name) LIKE LOWER(:q) ");
            binds.put("q", "%" + filter.q().trim() + "%");
        }
        if (filter.primaryOrganizationId() != null) {
            clause.append(" AND p.primary_organization_id = :primaryOrganizationId ");
            binds.put("primaryOrganizationId", filter.primaryOrganizationId());
        }
        if (filter.externalIdentifierType() != null && filter.externalIdentifierValue() != null) {
            clause.append("""
                     AND EXISTS (
                        SELECT 1 FROM clinical.patient_external_identifier e
                        WHERE e.patient_id = p.patient_id
                          AND e.identifier_type = :externalIdentifierType
                          AND e.identifier_value = :externalIdentifierValue
                     )
                    """);
            binds.put("externalIdentifierType", filter.externalIdentifierType().trim().toUpperCase()
                    .replace(' ', '_').replace('-', '_'));
            binds.put("externalIdentifierValue", filter.externalIdentifierValue().trim());
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

    private static String statusClause(PatientListFilter filter) {
        return switch (filter) {
            case ACTIVE -> " AND p.status = 'ACTIVE' ";
            case ARCHIVED -> " AND p.status = 'ARCHIVED' ";
            case ALL -> "";
        };
    }

    private record FilterSql(String clause, Map<String, Object> binds) {
    }

    private record PatientRow(
            UUID patientId,
            UUID tenantId,
            String displayName,
            String contactEmail,
            String contactPhone,
            LocalDate dateOfBirth,
            UUID primaryOrganizationId,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
