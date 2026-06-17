package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.IdentityAdminQueryRepository;
import com.codecore.iam.application.query.PageQuery;
import com.codecore.iam.application.query.PageQueryParser;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.persistence.mapper.IamUserMapper;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Membership-first identity listing for tenant administration (PASO 15.1).
 */
@Repository
public class R2dbcIdentityAdminQueryRepository implements IdentityAdminQueryRepository {

    private final DatabaseClient databaseClient;
    private final IamUserMapper iamUserMapper;

    public R2dbcIdentityAdminQueryRepository(ConnectionFactory connectionFactory, IamUserMapper iamUserMapper) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
        this.iamUserMapper = iamUserMapper;
    }

    @Override
    public Flux<Identity> findByTenantMembership(TenantId tenantId, PageQuery pageQuery) {
        String orderColumn = PageQueryParser.sqlOrderColumn(pageQuery.sortField());
        String direction = pageQuery.sortDirection() == PageQuery.SortDirection.ASC ? "ASC" : "DESC";

        return databaseClient.sql("""
                        SELECT u.id, u.tenant_id, u.email, u.normalized_email, u.password_hash, u.status,
                               u.email_verified, u.last_login_at, u.created_at, u.updated_at, u.version
                        FROM iam.iam_user u
                        INNER JOIN iam.identity_tenant_membership m ON m.identity_id = u.id
                        WHERE m.tenant_id = :tenantId
                        ORDER BY %s %s
                        LIMIT :limit OFFSET :offset
                        """.formatted(orderColumn, direction))
                .bind("tenantId", tenantId.value())
                .bind("limit", pageQuery.size())
                .bind("offset", pageQuery.offset())
                .map((row, metadata) -> iamUserMapper.toDomain(mapRow(row)))
                .all();
    }

    @Override
    public Mono<Long> countByTenantMembership(TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*)
                        FROM iam.iam_user u
                        INNER JOIN iam.identity_tenant_membership m ON m.identity_id = u.id
                        WHERE m.tenant_id = :tenantId
                        """)
                .bind("tenantId", tenantId.value())
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    private static com.codecore.iam.infrastructure.persistence.entity.IamUserEntity mapRow(
            io.r2dbc.spi.Readable row
    ) {
        com.codecore.iam.infrastructure.persistence.entity.IamUserEntity entity =
                new com.codecore.iam.infrastructure.persistence.entity.IamUserEntity();
        entity.setId(row.get("id", UUID.class));
        entity.setTenantId(row.get("tenant_id", UUID.class));
        entity.setEmail(row.get("email", String.class));
        entity.setNormalizedEmail(row.get("normalized_email", String.class));
        entity.setPasswordHash(row.get("password_hash", String.class));
        entity.setStatus(row.get("status", String.class));
        entity.setEmailVerifiedProjection(Boolean.TRUE.equals(row.get("email_verified", Boolean.class)));
        entity.setLastLoginAt(row.get("last_login_at", java.time.Instant.class));
        entity.setCreatedAt(row.get("created_at", java.time.Instant.class));
        entity.setUpdatedAt(row.get("updated_at", java.time.Instant.class));
        entity.setVersion(row.get("version", Long.class));
        return entity;
    }
}
