package com.codecore.encounter.configuration;

import com.codecore.encounter.infrastructure.persistence.mapper.EncounterMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Clinical Records (Encounter) module Spring entry point — persistence (FASE 19.4).
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.encounter.infrastructure.persistence.repository")
public class EncounterModuleConfiguration {

    @Bean
    public EncounterMapper encounterMapper() {
        return new EncounterMapper();
    }
}
