package com.codecore.payment.testsupport;

import com.codecore.payment.infrastructure.persistence.mapper.PaymentMapper;
import com.codecore.payment.infrastructure.persistence.repository.R2dbcPaymentAdminQueryRepository;
import com.codecore.payment.infrastructure.persistence.repository.R2dbcPaymentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.payment.infrastructure.persistence.repository")
@Import({R2dbcPaymentRepository.class, R2dbcPaymentAdminQueryRepository.class})
public class PaymentPersistenceTestConfiguration {

    @Bean
    PaymentMapper paymentMapper() {
        return new PaymentMapper();
    }
}
