package com.codecore.patient.configuration;

import com.codecore.patient.infrastructure.persistence.mapper.PatientMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Patient Management module Spring entry point — persistence adapters (FASE 17.4).
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.patient.infrastructure.persistence.repository")
public class PatientModuleConfiguration {

    @Bean
    public PatientMapper patientMapper() {
        return new PatientMapper();
    }
}
