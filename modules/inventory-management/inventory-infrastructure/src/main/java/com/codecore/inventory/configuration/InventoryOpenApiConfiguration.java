package com.codecore.inventory.configuration;

import com.codecore.inventory.interfaces.http.admin.ItemAdminApiPaths;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryOpenApiConfiguration {

    public static final String INVENTORY_ADMINISTRATION_GROUP = "inventory-administration";

    @Bean
    public GroupedOpenApi inventoryAdministrationGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group(INVENTORY_ADMINISTRATION_GROUP)
                .displayName("Inventory Administration")
                .pathsToMatch(ItemAdminApiPaths.BASE + "/**")
                .build();
    }
}
