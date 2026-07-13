package com.codecore.billing.configuration;

import com.codecore.billing.infrastructure.persistence.mapper.InvoiceMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Billing module Spring entry point — persistence + administration (FASE 21.4 / 21.6).
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.billing.infrastructure.persistence.repository")
@Import({BillingAdministrationConfiguration.class, BillingOpenApiConfiguration.class})
public class BillingModuleConfiguration {

    @Bean
    public InvoiceMapper invoiceMapper() {
        return new InvoiceMapper();
    }
}
