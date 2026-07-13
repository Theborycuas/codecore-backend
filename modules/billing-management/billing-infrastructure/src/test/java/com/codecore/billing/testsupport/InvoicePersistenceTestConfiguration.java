package com.codecore.billing.testsupport;

import com.codecore.billing.infrastructure.persistence.mapper.InvoiceMapper;
import com.codecore.billing.infrastructure.persistence.repository.R2dbcInvoiceAdminQueryRepository;
import com.codecore.billing.infrastructure.persistence.repository.R2dbcInvoiceRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.billing.infrastructure.persistence.repository")
@Import({R2dbcInvoiceRepository.class, R2dbcInvoiceAdminQueryRepository.class})
public class InvoicePersistenceTestConfiguration {

    @Bean
    InvoiceMapper invoiceMapper() {
        return new InvoiceMapper();
    }
}
