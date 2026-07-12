package com.codecore.inventory.domain.valueobject;

import com.codecore.inventory.domain.exception.InvalidDomainValueException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemValueObjectTest {

    @Test
    void shouldGenerateDistinctItemIds() {
        assertThat(ItemId.generate()).isNotEqualTo(ItemId.generate());
    }

    @Test
    void shouldParseItemIdFromString() {
        UUID uuid = UUID.randomUUID();
        assertThat(new ItemId(uuid.toString()).value()).isEqualTo(uuid);
    }

    @Test
    void shouldEqualTenantIdsByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(new TenantId(uuid)).isEqualTo(new TenantId(uuid.toString()));
    }

    @Test
    void shouldEqualPrimaryOrganizationIdsByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(PrimaryOrganizationId.of(uuid)).isEqualTo(new PrimaryOrganizationId(uuid.toString()));
    }

    @Test
    void shouldTrimDisplayName() {
        assertThat(ItemDisplayName.of("  Gloves  ").value()).isEqualTo("Gloves");
    }

    @Test
    void shouldRejectBlankDisplayName() {
        assertThatThrownBy(() -> ItemDisplayName.of("   "))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("display name");
    }

    @Test
    void shouldRejectOversizedDisplayName() {
        String oversized = "x".repeat(201);
        assertThatThrownBy(() -> ItemDisplayName.of(oversized))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("display name");
    }

    @Test
    void shouldTrimItemCode() {
        assertThat(ItemCode.of("  SKU-1  ").value()).isEqualTo("SKU-1");
    }

    @Test
    void shouldRejectBlankItemCode() {
        assertThatThrownBy(() -> ItemCode.of(" "))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("item code");
    }

    @Test
    void shouldRejectOversizedItemCode() {
        String oversized = "c".repeat(65);
        assertThatThrownBy(() -> ItemCode.of(oversized))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("item code");
    }

    @Test
    void shouldFreezeItemStatusEnum() {
        assertThat(ItemStatus.values()).containsExactly(ItemStatus.ACTIVE, ItemStatus.ARCHIVED);
    }
}
