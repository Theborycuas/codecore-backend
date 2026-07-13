package com.codecore.payment.configuration;

import com.codecore.payment.infrastructure.persistence.mapper.PaymentMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Payments module Spring entry point — persistence + administration (FASE 22.4 / 22.6).
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.payment.infrastructure.persistence.repository")
@Import({PaymentAdministrationConfiguration.class, PaymentOpenApiConfiguration.class})
public class PaymentModuleConfiguration {

    @Bean
    public PaymentMapper paymentMapper() {
        return new PaymentMapper();
    }
}
