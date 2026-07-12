package com.codecore.encounter.testsupport;

import com.codecore.encounter.infrastructure.persistence.mapper.EncounterMapper;
import com.codecore.encounter.infrastructure.persistence.repository.R2dbcEncounterRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.encounter.infrastructure.persistence.repository")
@Import(R2dbcEncounterRepository.class)
public class EncounterPersistenceTestConfiguration {

    @Bean
    EncounterMapper encounterMapper() {
        return new EncounterMapper();
    }
}
