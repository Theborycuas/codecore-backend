package com.codecore.patient.configuration;

import com.codecore.patient.infrastructure.persistence.mapper.PatientMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Patient Management module Spring entry point — persistence + administration (FASE 17.4 / 17.6).
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.patient.infrastructure.persistence.repository")
@Import({PatientAdministrationConfiguration.class, PatientOpenApiConfiguration.class})
public class PatientModuleConfiguration {

    @Bean
    public PatientMapper patientMapper() {
        return new PatientMapper();
    }
}
