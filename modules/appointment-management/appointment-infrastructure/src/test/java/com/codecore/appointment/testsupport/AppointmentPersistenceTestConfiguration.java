package com.codecore.appointment.testsupport;

import com.codecore.appointment.infrastructure.persistence.mapper.AppointmentMapper;
import com.codecore.appointment.infrastructure.persistence.repository.R2dbcAppointmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.appointment.infrastructure.persistence.repository")
@Import(R2dbcAppointmentRepository.class)
public class AppointmentPersistenceTestConfiguration {

    @Bean
    AppointmentMapper appointmentMapper() {
        return new AppointmentMapper();
    }
}
