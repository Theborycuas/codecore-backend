package com.codecore.patient.testsupport;

import com.codecore.patient.infrastructure.persistence.mapper.PatientMapper;
import com.codecore.patient.infrastructure.persistence.repository.R2dbcPatientRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.patient.infrastructure.persistence.repository")
@Import(R2dbcPatientRepository.class)
public class PatientPersistenceTestConfiguration {

    @Bean
    PatientMapper patientMapper() {
        return new PatientMapper();
    }
}
