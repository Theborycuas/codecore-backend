package com.codecore.inventory.configuration;

import com.codecore.inventory.infrastructure.persistence.mapper.ItemMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Inventory module Spring entry point — persistence + administration (FASE 20.4 / 20.6).
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.inventory.infrastructure.persistence.repository")
@Import({InventoryAdministrationConfiguration.class, InventoryOpenApiConfiguration.class})
public class InventoryModuleConfiguration {

    @Bean
    public ItemMapper itemMapper() {
        return new ItemMapper();
    }
}
