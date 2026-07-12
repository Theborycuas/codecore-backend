package com.codecore.inventory.testsupport;

import com.codecore.inventory.infrastructure.persistence.mapper.ItemMapper;
import com.codecore.inventory.infrastructure.persistence.repository.R2dbcItemRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.inventory.infrastructure.persistence.repository")
@Import(R2dbcItemRepository.class)
public class ItemPersistenceTestConfiguration {

    @Bean
    ItemMapper itemMapper() {
        return new ItemMapper();
    }
}
