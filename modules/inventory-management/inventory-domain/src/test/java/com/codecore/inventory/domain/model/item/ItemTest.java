package com.codecore.inventory.domain.model.item;

import com.codecore.inventory.domain.exception.InvalidItemStateException;
import com.codecore.inventory.domain.valueobject.ItemCode;
import com.codecore.inventory.domain.valueobject.ItemDisplayName;
import com.codecore.inventory.domain.valueobject.ItemId;
import com.codecore.inventory.domain.valueobject.ItemStatus;
import com.codecore.inventory.domain.valueobject.PrimaryOrganizationId;
import com.codecore.inventory.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemTest {

    private static final Instant NOW = Instant.parse("2026-07-12T18:00:00Z");

    @Test
    void shouldCreateValidItem() {
        ItemId id = ItemId.generate();
        TenantId tenantId = TenantId.generate();
        ItemDisplayName displayName = ItemDisplayName.of("Nitrile Gloves M");

        Item item = Item.create(id, tenantId, displayName, NOW);

        assertThat(item.id()).isEqualTo(id);
        assertThat(item.tenantId()).isEqualTo(tenantId);
        assertThat(item.displayName()).isEqualTo(displayName);
        assertThat(item.code()).isEmpty();
        assertThat(item.primaryOrganizationId()).isEmpty();
        assertThat(item.status()).isEqualTo(ItemStatus.ACTIVE);
        assertThat(item.createdAt()).isEqualTo(NOW);
        assertThat(item.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldCreateWithCodeAndPrimaryOrganization() {
        ItemCode code = ItemCode.of("SKU-GLOVE-M");
        PrimaryOrganizationId orgId = PrimaryOrganizationId.of(UUID.randomUUID());

        Item item = Item.create(
                ItemId.generate(),
                TenantId.generate(),
                ItemDisplayName.of("Nitrile Gloves M"),
                code,
                orgId,
                NOW
        );

        assertThat(item.code()).contains(code);
        assertThat(item.primaryOrganizationId()).contains(orgId);
        assertThat(item.status()).isEqualTo(ItemStatus.ACTIVE);
    }

    @Test
    void shouldRequireTenantId() {
        assertThatThrownBy(() -> Item.create(
                ItemId.generate(),
                null,
                ItemDisplayName.of("Composite Resin"),
                NOW
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void shouldKeepTenantIdImmutableAfterMutations() {
        Item item = activeItem();
        TenantId original = item.tenantId();

        item.rename(ItemDisplayName.of("Renamed"));
        item.assignCode(ItemCode.of("SKU-1"));
        item.assignPrimaryOrganization(PrimaryOrganizationId.of(UUID.randomUUID()));
        item.removePrimaryOrganization();
        item.clearCode();
        item.archive();
        item.activate();

        assertThat(item.tenantId()).isEqualTo(original);
    }

    @Test
    void shouldRenameWhenActive() {
        Item item = activeItem();

        item.rename(ItemDisplayName.of("Updated Label"));

        assertThat(item.displayName().value()).isEqualTo("Updated Label");
        assertThat(item.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldAssignAndClearCodeWhenActive() {
        Item item = activeItem();
        ItemCode code = ItemCode.of("MAT-001");

        item.assignCode(code);
        assertThat(item.code()).contains(code);

        item.clearCode();
        assertThat(item.code()).isEmpty();
        assertThat(item.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldRejectNullCodeOnAssign() {
        Item item = activeItem();

        assertThatThrownBy(() -> item.assignCode(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("newCode");
    }

    @Test
    void shouldAssignAndRemovePrimaryOrganization() {
        Item item = activeItem();
        PrimaryOrganizationId orgId = PrimaryOrganizationId.of(UUID.randomUUID());

        item.assignPrimaryOrganization(orgId);
        assertThat(item.primaryOrganizationId()).contains(orgId);

        item.removePrimaryOrganization();
        assertThat(item.primaryOrganizationId()).isEmpty();
        assertThat(item.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldRejectNullPrimaryOrganizationOnAssign() {
        Item item = activeItem();

        assertThatThrownBy(() -> item.assignPrimaryOrganization(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("organizationId");
    }

    @Test
    void shouldArchiveAndActivate() {
        Item item = activeItem();

        item.archive();
        assertThat(item.status()).isEqualTo(ItemStatus.ARCHIVED);

        item.activate();
        assertThat(item.status()).isEqualTo(ItemStatus.ACTIVE);
        assertThat(item.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldRejectArchiveWhenAlreadyArchived() {
        Item item = activeItem();
        item.archive();

        assertThatThrownBy(item::archive)
                .isInstanceOf(InvalidItemStateException.class)
                .hasMessageContaining("already archived");
    }

    @Test
    void shouldRejectActivateWhenAlreadyActive() {
        Item item = activeItem();

        assertThatThrownBy(item::activate)
                .isInstanceOf(InvalidItemStateException.class)
                .hasMessageContaining("already active");
    }

    @Test
    void shouldRejectRenameWhenArchived() {
        Item item = activeItem();
        item.archive();

        assertThatThrownBy(() -> item.rename(ItemDisplayName.of("X")))
                .isInstanceOf(InvalidItemStateException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void shouldRejectCodeAndOrganizationMutationWhenArchived() {
        Item item = activeItem();
        item.archive();

        assertThatThrownBy(() -> item.assignCode(ItemCode.of("X")))
                .isInstanceOf(InvalidItemStateException.class)
                .hasMessageContaining("archived");

        assertThatThrownBy(item::clearCode)
                .isInstanceOf(InvalidItemStateException.class)
                .hasMessageContaining("archived");

        assertThatThrownBy(() -> item.assignPrimaryOrganization(PrimaryOrganizationId.of(UUID.randomUUID())))
                .isInstanceOf(InvalidItemStateException.class)
                .hasMessageContaining("archived");

        assertThatThrownBy(item::removePrimaryOrganization)
                .isInstanceOf(InvalidItemStateException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void shouldReconstituteItem() {
        ItemId id = ItemId.generate();
        TenantId tenantId = TenantId.generate();
        ItemDisplayName displayName = ItemDisplayName.of("Historic Resin");
        ItemCode code = ItemCode.of("OLD-SKU");
        PrimaryOrganizationId orgId = PrimaryOrganizationId.of(UUID.randomUUID());
        Instant createdAt = NOW.minusSeconds(3600);
        Instant updatedAt = NOW.minusSeconds(60);

        Item item = Item.reconstitute(
                id,
                tenantId,
                displayName,
                code,
                orgId,
                ItemStatus.ARCHIVED,
                createdAt,
                updatedAt
        );

        assertThat(item.id()).isEqualTo(id);
        assertThat(item.tenantId()).isEqualTo(tenantId);
        assertThat(item.displayName()).isEqualTo(displayName);
        assertThat(item.code()).contains(code);
        assertThat(item.primaryOrganizationId()).contains(orgId);
        assertThat(item.status()).isEqualTo(ItemStatus.ARCHIVED);
        assertThat(item.createdAt()).isEqualTo(createdAt);
        assertThat(item.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldNeverExposeStockPricingOrClinicalConcernsInPublicApi() {
        assertThat(Item.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain(
                        "addStock",
                        "setQuantity",
                        "adjustStock",
                        "setPrice",
                        "addBom",
                        "addLot",
                        "assignOffice",
                        "setOfficeId",
                        "linkEncounter",
                        "linkPatient",
                        "addSupplier",
                        "setUnitOfMeasure"
                );
    }

    private static Item activeItem() {
        return Item.create(
                ItemId.generate(),
                TenantId.generate(),
                ItemDisplayName.of("Nitrile Gloves M"),
                NOW
        );
    }
}
