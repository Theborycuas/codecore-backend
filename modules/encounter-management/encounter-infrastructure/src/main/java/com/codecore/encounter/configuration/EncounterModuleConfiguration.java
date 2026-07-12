package com.codecore.encounter.configuration;

import com.codecore.encounter.infrastructure.persistence.mapper.EncounterMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Clinical Records (Encounter) module Spring entry point — persistence + administration (FASE 19.4 / 19.6).
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.encounter.infrastructure.persistence.repository")
@Import({EncounterAdministrationConfiguration.class, EncounterOpenApiConfiguration.class})
public class EncounterModuleConfiguration {

    @Bean
    public EncounterMapper encounterMapper() {
        return new EncounterMapper();
    }
}
