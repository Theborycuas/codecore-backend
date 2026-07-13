package com.codecore.billing.infrastructure.persistence.repository;

import com.codecore.billing.application.dto.AdminInvoiceLineView;
import com.codecore.billing.application.dto.AdminInvoiceView;
import com.codecore.billing.application.port.out.InvoiceAdminQueryRepository;
import com.codecore.billing.application.query.InvoiceListFilter;
import com.codecore.billing.application.query.InvoiceListQuery;
import com.codecore.billing.application.query.PageQuery;
import com.codecore.billing.application.query.PageQueryParser;
import com.codecore.billing.domain.valueobject.BillToOrganizationId;
import com.codecore.billing.domain.valueobject.BillToPatientId;
import com.codecore.billing.domain.valueobject.EncounterId;
import com.codecore.billing.domain.valueobject.InvoiceId;
import com.codecore.billing.domain.valueobject.InvoiceLineId;
import com.codecore.billing.domain.valueobject.InvoiceStatus;
import com.codecore.billing.domain.valueobject.ItemId;
import com.codecore.billing.domain.valueobject.OrganizationId;
import com.codecore.billing.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class R2dbcInvoiceAdminQueryRepository implements InvoiceAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcInvoiceAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminInvoiceView> findByTenantId(TenantId tenantId, InvoiceListQuery filter, PageQuery pageQuery) {
        String orderColumn = PageQueryParser.invoiceSqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";
        FilterSql filterSql = buildFilterSql(filter);

        return bindFilter(databaseClient.sql("""
                        SELECT i.invoice_id, i.tenant_id, i.issuer_organization_id,
                               i.bill_to_patient_id, i.bill_to_organization_id, i.invoice_number,
                               i.currency, i.status, i.created_at, i.updated_at
                        FROM billing.invoice i
                        WHERE i.tenant_id = :tenantId
                        %s
                        ORDER BY i.%s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(filterSql.clause(), orderColumn, direction)), filterSql)
                .bind("tenantId", tenantId.value())
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset())
                .map((row, metadata) -> toRow(
                        row.get("invoice_id", UUID.class),
                        row.get("tenant_id", UUID.class),
                        row.get("issuer_organization_id", UUID.class),
                        row.get("bill_to_patient_id", UUID.class),
                        row.get("bill_to_organization_id", UUID.class),
                        row.get("invoice_number", String.class),
                        row.get("currency", String.class),
                        row.get("status", String.class),
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class)
                ))
                .all()
                .concatMap(this::attachLines);
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId, InvoiceListQuery filter) {
        FilterSql filterSql = buildFilterSql(filter);
        return bindFilter(databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM billing.invoice i
                        WHERE i.tenant_id = :tenantId
                        %s
                        """.formatted(filterSql.clause())), filterSql)
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private Mono<AdminInvoiceView> attachLines(InvoiceRow row) {
        return fetchLines(row.invoiceId()).collectList().map(lines -> toView(row, lines));
    }

    private Flux<AdminInvoiceLineView> fetchLines(UUID invoiceId) {
        return databaseClient.sql("""
                        SELECT line_id, description, amount_minor, currency, item_id, encounter_id
                        FROM billing.invoice_line
                        WHERE invoice_id = :invoiceId
                        ORDER BY line_id
                        """)
                .bind("invoiceId", invoiceId)
                .map((row, metadata) -> new AdminInvoiceLineView(
                        new InvoiceLineId(row.get("line_id", UUID.class)),
                        row.get("description", String.class),
                        row.get("amount_minor", Long.class),
                        row.get("currency", String.class),
                        row.get("item_id", UUID.class) == null ? null : ItemId.of(row.get("item_id", UUID.class)),
                        row.get("encounter_id", UUID.class) == null
                                ? null
                                : EncounterId.of(row.get("encounter_id", UUID.class))
                ))
                .all();
    }

    private static InvoiceRow toRow(
            UUID invoiceId,
            UUID tenantId,
            UUID issuerOrganizationId,
            UUID billToPatientId,
            UUID billToOrganizationId,
            String invoiceNumber,
            String currency,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new InvoiceRow(
                invoiceId,
                tenantId,
                issuerOrganizationId,
                billToPatientId,
                billToOrganizationId,
                invoiceNumber,
                currency,
                status,
                createdAt,
                updatedAt
        );
    }

    private static AdminInvoiceView toView(InvoiceRow row, List<AdminInvoiceLineView> lines) {
        long totalAmountMinor = lines.stream().mapToLong(AdminInvoiceLineView::amountMinor).sum();
        return new AdminInvoiceView(
                new InvoiceId(row.invoiceId()),
                new TenantId(row.tenantId()),
                OrganizationId.of(row.issuerOrganizationId()),
                row.billToPatientId() == null ? null : BillToPatientId.of(row.billToPatientId()),
                row.billToOrganizationId() == null ? null : BillToOrganizationId.of(row.billToOrganizationId()),
                row.invoiceNumber(),
                row.currency(),
                lines,
                totalAmountMinor,
                InvoiceStatus.valueOf(row.status()),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private static FilterSql buildFilterSql(InvoiceListQuery filter) {
        StringBuilder clause = new StringBuilder();
        clause.append(statusClause(filter.status()));
        Map<String, Object> binds = new HashMap<>();
        if (filter.q() != null) {
            clause.append(" AND LOWER(i.invoice_number) LIKE LOWER(:q) ");
            binds.put("q", "%" + filter.q().trim() + "%");
        }
        if (filter.issuerOrganizationId() != null) {
            clause.append(" AND i.issuer_organization_id = :issuerOrganizationId ");
            binds.put("issuerOrganizationId", filter.issuerOrganizationId());
        }
        if (filter.billToPatientId() != null) {
            clause.append(" AND i.bill_to_patient_id = :billToPatientId ");
            binds.put("billToPatientId", filter.billToPatientId());
        }
        if (filter.billToOrganizationId() != null) {
            clause.append(" AND i.bill_to_organization_id = :billToOrganizationId ");
            binds.put("billToOrganizationId", filter.billToOrganizationId());
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

    private static String statusClause(InvoiceListFilter filter) {
        return switch (filter) {
            case DRAFT -> " AND i.status = 'DRAFT' ";
            case ISSUED -> " AND i.status = 'ISSUED' ";
            case VOIDED -> " AND i.status = 'VOIDED' ";
            case ALL -> "";
        };
    }

    private record FilterSql(String clause, Map<String, Object> binds) {
    }

    private record InvoiceRow(
            UUID invoiceId,
            UUID tenantId,
            UUID issuerOrganizationId,
            UUID billToPatientId,
            UUID billToOrganizationId,
            String invoiceNumber,
            String currency,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
