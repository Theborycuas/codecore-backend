package com.codecore.inventory.infrastructure.persistence.repository;

import com.codecore.inventory.application.port.out.ItemQueryPort;
import com.codecore.inventory.application.port.out.ItemRepository;
import com.codecore.inventory.domain.model.item.Item;
import com.codecore.inventory.domain.valueobject.ItemCode;
import com.codecore.inventory.domain.valueobject.ItemDisplayName;
import com.codecore.inventory.domain.valueobject.ItemId;
import com.codecore.inventory.domain.valueobject.ItemStatus;
import com.codecore.inventory.domain.valueobject.PrimaryOrganizationId;
import com.codecore.inventory.domain.valueobject.TenantId;
import com.codecore.inventory.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.inventory.testsupport.ItemPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(ItemPersistenceTestConfiguration.class)
class R2dbcItemRepositoryIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-12T19:00:00Z");

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemQueryPort itemQueryPort;

    @Test
    void shouldPersistAndFindById() {
        ItemId itemId = ItemId.generate();
        TenantId tenantId = TenantId.generate();
        ItemCode code = ItemCode.of("SKU-GLOVE-M");
        PrimaryOrganizationId orgId = PrimaryOrganizationId.of(UUID.randomUUID());

        Item item = Item.create(
                itemId,
                tenantId,
                ItemDisplayName.of("Nitrile Gloves M"),
                code,
                orgId,
                NOW
        );

        StepVerifier.create(itemRepository.save(item))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(itemId);
                    assertThat(saved.tenantId()).isEqualTo(tenantId);
                    assertThat(saved.displayName().value()).isEqualTo("Nitrile Gloves M");
                    assertThat(saved.code()).contains(code);
                    assertThat(saved.primaryOrganizationId()).contains(orgId);
                    assertThat(saved.status()).isEqualTo(ItemStatus.ACTIVE);
                })
                .verifyComplete();

        StepVerifier.create(itemRepository.findById(itemId))
                .assertNext(found -> {
                    assertThat(found.id()).isEqualTo(itemId);
                    assertThat(found.code()).contains(code);
                    assertThat(found.status()).isEqualTo(ItemStatus.ACTIVE);
                })
                .verifyComplete();
    }

    @Test
    void shouldReportExistsByIdAndTenant() {
        ItemId itemId = ItemId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(itemRepository.save(activeItem(itemId, tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(itemRepository.existsById(itemId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(itemRepository.existsByIdAndTenantId(itemId, tenantId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(itemRepository.existsByIdAndTenantId(itemId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(itemRepository.existsById(ItemId.generate()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldCountAndFindByTenantId() {
        TenantId tenantId = TenantId.generate();
        TenantId otherTenantId = TenantId.generate();

        StepVerifier.create(itemRepository.save(activeItem(ItemId.generate(), tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(itemRepository.save(activeItem(ItemId.generate(), tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(itemRepository.save(activeItem(ItemId.generate(), otherTenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(itemQueryPort.countByTenantId(tenantId))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(itemQueryPort.findByTenantId(tenantId))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldIsolateCrossTenantReads() {
        ItemId itemId = ItemId.generate();
        TenantId tenantId = TenantId.generate();
        TenantId otherTenantId = TenantId.generate();

        StepVerifier.create(itemRepository.save(activeItem(itemId, tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(itemQueryPort.findByIdAndTenantId(itemId, tenantId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(itemId))
                .verifyComplete();

        StepVerifier.create(itemQueryPort.findByIdAndTenantId(itemId, otherTenantId))
                .verifyComplete();
    }

    @Test
    void shouldPersistNullAndNonNullCodeAndPrimaryOrganization() {
        TenantId tenantId = TenantId.generate();
        ItemCode code = ItemCode.of("RESIN-A2");
        PrimaryOrganizationId orgId = PrimaryOrganizationId.of(UUID.randomUUID());

        StepVerifier.create(itemRepository.save(activeItem(ItemId.generate(), tenantId, null, null)))
                .assertNext(saved -> {
                    assertThat(saved.code()).isEmpty();
                    assertThat(saved.primaryOrganizationId()).isEmpty();
                })
                .verifyComplete();

        StepVerifier.create(itemRepository.save(activeItem(ItemId.generate(), tenantId, code, orgId)))
                .assertNext(saved -> {
                    assertThat(saved.code()).contains(code);
                    assertThat(saved.primaryOrganizationId()).contains(orgId);
                })
                .verifyComplete();
    }

    @Test
    void shouldFindByTenantIdAndPrimaryOrganizationId() {
        TenantId tenantId = TenantId.generate();
        PrimaryOrganizationId orgId = PrimaryOrganizationId.of(UUID.randomUUID());
        ItemId matchingId = ItemId.generate();

        StepVerifier.create(itemRepository.save(activeItem(matchingId, tenantId, null, orgId)))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(itemRepository.save(activeItem(ItemId.generate(), tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(itemQueryPort.findByTenantIdAndPrimaryOrganizationId(tenantId, orgId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(matchingId))
                .verifyComplete();
    }

    @Test
    void shouldFindByTenantIdAndStatus() {
        TenantId tenantId = TenantId.generate();
        ItemId activeId = ItemId.generate();
        ItemId archivedId = ItemId.generate();

        StepVerifier.create(itemRepository.save(activeItem(activeId, tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        Item archived = activeItem(archivedId, tenantId, null, null);
        archived.archive();
        StepVerifier.create(itemRepository.save(archived))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(itemQueryPort.findByTenantIdAndStatus(tenantId, ItemStatus.ACTIVE))
                .assertNext(found -> assertThat(found.id()).isEqualTo(activeId))
                .verifyComplete();

        StepVerifier.create(itemQueryPort.findByTenantIdAndStatus(tenantId, ItemStatus.ARCHIVED))
                .assertNext(found -> assertThat(found.id()).isEqualTo(archivedId))
                .verifyComplete();
    }

    @Test
    void shouldEnforceSoftUniqueCodeWithinSameTenant() {
        TenantId tenantId = TenantId.generate();
        ItemCode code = ItemCode.of("DUP-SKU");

        StepVerifier.create(itemRepository.save(activeItem(ItemId.generate(), tenantId, code, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(itemRepository.save(activeItem(ItemId.generate(), tenantId, code, null)))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    @Test
    void shouldAllowSameCodeInDifferentTenants() {
        ItemCode code = ItemCode.of("SHARED-SKU");

        StepVerifier.create(itemRepository.save(activeItem(ItemId.generate(), TenantId.generate(), code, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(itemRepository.save(activeItem(ItemId.generate(), TenantId.generate(), code, null)))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldAllowMultipleItemsWithoutCode() {
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(itemRepository.save(activeItem(ItemId.generate(), tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(itemRepository.save(activeItem(ItemId.generate(), tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(itemQueryPort.countByTenantId(tenantId))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void shouldReportCodeExistenceHelpers() {
        TenantId tenantId = TenantId.generate();
        ItemId itemId = ItemId.generate();
        ItemCode code = ItemCode.of("EXIST-SKU");

        StepVerifier.create(itemRepository.save(activeItem(itemId, tenantId, code, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(itemQueryPort.existsByTenantIdAndCode(tenantId, code))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(itemQueryPort.existsByTenantIdAndCodeExcludingId(tenantId, code, itemId))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(itemQueryPort.existsByTenantIdAndCodeExcludingId(
                        tenantId, code, ItemId.generate()))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(itemQueryPort.existsByTenantIdAndCode(tenantId, ItemCode.of("MISSING")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldUpdateExistingItemWithoutDuplicatingRow() {
        ItemId itemId = ItemId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(itemRepository.save(activeItem(itemId, tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(itemRepository.findById(itemId)
                        .flatMap(loaded -> {
                            loaded.rename(ItemDisplayName.of("Updated Gloves"));
                            loaded.assignCode(ItemCode.of("UPD-SKU"));
                            return itemRepository.save(loaded);
                        }))
                .assertNext(saved -> {
                    assertThat(saved.displayName().value()).isEqualTo("Updated Gloves");
                    assertThat(saved.code()).contains(ItemCode.of("UPD-SKU"));
                })
                .verifyComplete();

        StepVerifier.create(itemQueryPort.countByTenantId(tenantId))
                .expectNext(1L)
                .verifyComplete();
    }

    private static Item activeItem(
            ItemId itemId,
            TenantId tenantId,
            ItemCode code,
            PrimaryOrganizationId primaryOrganizationId
    ) {
        return Item.create(
                itemId,
                tenantId,
                ItemDisplayName.of("Catalog Item"),
                code,
                primaryOrganizationId,
                NOW
        );
    }
}
