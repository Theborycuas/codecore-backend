package com.codecore.appointment.configuration;

import com.codecore.appointment.infrastructure.persistence.mapper.AppointmentMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Scheduling (Appointment) module Spring entry point — persistence + administration (FASE 18.4 / 18.6).
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.appointment.infrastructure.persistence.repository")
@Import({AppointmentAdministrationConfiguration.class, AppointmentOpenApiConfiguration.class})
public class AppointmentModuleConfiguration {

    @Bean
    public AppointmentMapper appointmentMapper() {
        return new AppointmentMapper();
    }
}
