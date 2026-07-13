package com.codecore.payment.infrastructure.persistence.repository;

import com.codecore.payment.application.dto.AdminPaymentView;
import com.codecore.payment.application.port.out.PaymentAdminQueryRepository;
import com.codecore.payment.application.query.PageQuery;
import com.codecore.payment.application.query.PageQueryParser;
import com.codecore.payment.application.query.PaymentListFilter;
import com.codecore.payment.application.query.PaymentListQuery;
import com.codecore.payment.domain.valueobject.InvoiceId;
import com.codecore.payment.domain.valueobject.PaymentId;
import com.codecore.payment.domain.valueobject.PaymentStatus;
import com.codecore.payment.domain.valueobject.TenantId;
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
public class R2dbcPaymentAdminQueryRepository implements PaymentAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcPaymentAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminPaymentView> findByTenantId(TenantId tenantId, PaymentListQuery filter, PageQuery pageQuery) {
        String orderColumn = PageQueryParser.paymentSqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";
        FilterSql filterSql = buildFilterSql(filter);

        return bindFilter(databaseClient.sql("""
                        SELECT p.payment_id, p.tenant_id, p.invoice_id, p.currency, p.amount_minor,
                               p.payment_method_code, p.recorded_at, p.status, p.created_at, p.updated_at
                        FROM payments.payment p
                        WHERE p.tenant_id = :tenantId
                        %s
                        ORDER BY p.%s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(filterSql.clause(), orderColumn, direction)), filterSql)
                .bind("tenantId", tenantId.value())
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset())
                .map((row, metadata) -> toView(
                        row.get("payment_id", UUID.class),
                        row.get("tenant_id", UUID.class),
                        row.get("invoice_id", UUID.class),
                        row.get("currency", String.class),
                        row.get("amount_minor", Long.class),
                        row.get("payment_method_code", String.class),
                        row.get("recorded_at", Instant.class),
                        row.get("status", String.class),
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId, PaymentListQuery filter) {
        FilterSql filterSql = buildFilterSql(filter);
        return bindFilter(databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM payments.payment p
                        WHERE p.tenant_id = :tenantId
                        %s
                        """.formatted(filterSql.clause())), filterSql)
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private static AdminPaymentView toView(
            UUID paymentId,
            UUID tenantId,
            UUID invoiceId,
            String currency,
            long amountMinor,
            String paymentMethodCode,
            Instant recordedAt,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new AdminPaymentView(
                new PaymentId(paymentId),
                new TenantId(tenantId),
                new InvoiceId(invoiceId),
                currency,
                amountMinor,
                paymentMethodCode,
                recordedAt,
                PaymentStatus.valueOf(status),
                createdAt,
                updatedAt
        );
    }

    private static FilterSql buildFilterSql(PaymentListQuery filter) {
        StringBuilder clause = new StringBuilder();
        clause.append(statusClause(filter.status()));
        Map<String, Object> binds = new HashMap<>();
        if (filter.invoiceId() != null) {
            clause.append(" AND p.invoice_id = :invoiceId ");
            binds.put("invoiceId", filter.invoiceId());
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

    private static String statusClause(PaymentListFilter filter) {
        return switch (filter) {
            case RECORDED -> " AND p.status = 'RECORDED' ";
            case VOIDED -> " AND p.status = 'VOIDED' ";
            case ALL -> "";
        };
    }

    private record FilterSql(String clause, Map<String, Object> binds) {
    }
}
