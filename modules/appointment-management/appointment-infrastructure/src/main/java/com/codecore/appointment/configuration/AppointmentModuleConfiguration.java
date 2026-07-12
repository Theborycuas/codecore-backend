package com.codecore.appointment.configuration;

import com.codecore.appointment.infrastructure.persistence.mapper.AppointmentMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Spring wiring for Scheduling (Appointment) persistence (PASO 18.4).
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.appointment.infrastructure.persistence.repository")
public class AppointmentModuleConfiguration {

    @Bean
    public AppointmentMapper appointmentMapper() {
        return new AppointmentMapper();
    }
}
