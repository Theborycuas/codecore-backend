package com.codecore.inventory.contract.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemPermissionCatalogTest {

    @Test
    void shouldExposeFourItemLifecyclePermissions() {
        assertThat(ItemPermissionCatalog.ALL).hasSize(4);
        assertThat(ItemPermissionCatalog.ITEM_READ_ONLY)
                .containsExactly(ItemPermissionCatalog.ITEM_READ);
    }

    @Test
    void shouldUseArchiveNotDeleteAndMapActivateToUpdate() {
        assertThat(ItemPermissionCatalog.ALL)
                .contains(ItemPermissionCatalog.ITEM_ARCHIVE)
                .contains(ItemPermissionCatalog.ITEM_UPDATE)
                .doesNotContain("item:delete")
                .doesNotContain("item:activate")
                .doesNotContain("item:restore")
                .doesNotContain("item:stock")
                .doesNotContain("item:adjust");
    }

    @Test
    void shouldRemainVerticalAgnostic() {
        assertThat(ItemPermissionCatalog.ALL).noneMatch(code ->
                code.contains("dental")
                        || code.contains("vet")
                        || code.contains("hospital")
                        || code.contains("retail")
                        || code.contains("sku-import")
                        || code.contains("bom")
                        || code.contains("price")
                        || code.contains("stock")
        );
    }
}
