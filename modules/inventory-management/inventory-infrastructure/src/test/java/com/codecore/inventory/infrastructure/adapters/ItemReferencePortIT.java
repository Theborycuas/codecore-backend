package com.codecore.inventory.infrastructure.adapters;

import com.codecore.inventory.application.port.out.ItemRepository;
import com.codecore.inventory.contract.reference.ItemReferencePort;
import com.codecore.inventory.domain.model.item.Item;
import com.codecore.inventory.domain.valueobject.ItemDisplayName;
import com.codecore.inventory.domain.valueobject.ItemId;
import com.codecore.inventory.domain.valueobject.TenantId;
import com.codecore.inventory.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.inventory.testsupport.ItemPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Instant;

@DataR2dbcTest
@Import({
        ItemPersistenceTestConfiguration.class,
        R2dbcItemReferenceAdapter.class
})
class ItemReferencePortIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-12T21:00:00Z");

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemReferencePort itemReferencePort;

    @Test
    void shouldReturnTrueForActiveItemInTenant() {
        ItemId itemId = ItemId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(itemRepository.save(Item.create(
                        itemId,
                        tenantId,
                        ItemDisplayName.of("Active Catalog Entry"),
                        NOW
                )))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(itemReferencePort.existsActiveByIdAndTenant(itemId, tenantId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseForWrongTenantOrUnknownId() {
        ItemId itemId = ItemId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(itemRepository.save(Item.create(
                        itemId,
                        tenantId,
                        ItemDisplayName.of("Scoped Catalog Entry"),
                        NOW
                )))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(itemReferencePort.existsActiveByIdAndTenant(itemId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(itemReferencePort.existsActiveByIdAndTenant(ItemId.generate(), tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenItemArchived() {
        ItemId itemId = ItemId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(itemRepository.save(Item.create(
                        itemId,
                        tenantId,
                        ItemDisplayName.of("Archived Catalog Entry"),
                        NOW
                )).flatMap(saved -> {
                    saved.archive();
                    return itemRepository.save(saved);
                }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(itemReferencePort.existsActiveByIdAndTenant(itemId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }
}
