package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.dto.AdminPermissionView;
import com.codecore.iam.application.port.out.PermissionAdminQueryRepository;
import com.codecore.iam.application.query.PageQuery;
import com.codecore.iam.application.query.PageQueryParser;
import com.codecore.iam.domain.valueobject.PermissionId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public class R2dbcPermissionAdminQueryRepository implements PermissionAdminQueryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcPermissionAdminQueryRepository(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Flux<AdminPermissionView> findAll(PageQuery pageQuery) {
        String orderColumn = PageQueryParser.permissionSqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";

        return databaseClient.sql("""
                        SELECT permission_id, code, description, system_permission, created_at, updated_at
                        FROM iam.permission
                        ORDER BY %s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(orderColumn, direction))
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset())
                .map((row, metadata) -> new AdminPermissionView(
                        new PermissionId(row.get("permission_id", UUID.class)),
                        row.get("code", String.class),
                        row.get("description", String.class),
                        row.get("system_permission", Boolean.class),
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Mono<Long> countAll() {
        return databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM iam.permission
                        """)
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }
}
