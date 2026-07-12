package com.codecore.inventory.infrastructure.adapters;

import com.codecore.inventory.contract.reference.ItemReferencePort;
import com.codecore.inventory.domain.valueobject.ItemId;
import com.codecore.inventory.domain.valueobject.TenantId;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * In-process adapter for {@link ItemReferencePort} (ADR-013 / ADR-016).
 */
@Component
public class R2dbcItemReferenceAdapter implements ItemReferencePort {

    private static final String ACTIVE = "ACTIVE";

    private final DatabaseClient databaseClient;

    public R2dbcItemReferenceAdapter(ConnectionFactory connectionFactory) {
        this.databaseClient = DatabaseClient.create(connectionFactory);
    }

    @Override
    public Mono<Boolean> existsActiveByIdAndTenant(ItemId itemId, TenantId tenantId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) > 0
                        FROM inventory.item
                        WHERE item_id = :itemId
                          AND tenant_id = :tenantId
                          AND status = :status
                        """)
                .bind("itemId", itemId.value())
                .bind("tenantId", tenantId.value())
                .bind("status", ACTIVE)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get(0, Boolean.class)))
                .one()
                .defaultIfEmpty(false);
    }
}
