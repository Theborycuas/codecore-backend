package com.codecore.inventory.configuration;

import com.codecore.inventory.infrastructure.persistence.mapper.ItemMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Inventory module Spring entry point — persistence (FASE 20.4).
 * Administration / OpenAPI imported in later pasos.
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.inventory.infrastructure.persistence.repository")
public class InventoryModuleConfiguration {

    @Bean
    public ItemMapper itemMapper() {
        return new ItemMapper();
    }
}
