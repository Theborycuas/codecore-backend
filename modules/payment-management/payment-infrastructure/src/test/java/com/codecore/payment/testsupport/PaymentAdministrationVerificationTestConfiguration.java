package com.codecore.payment.testsupport;

import com.codecore.iam.configuration.IamOpenApiConfiguration;
import com.codecore.payment.configuration.PaymentOpenApiConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Full Payment administration stack for FASE 22.7 verification (E2E HTTP).
 */
@Configuration
@Import({
        PaymentAdminIntegrationTestConfiguration.class,
        PaymentOpenApiConfiguration.class,
        IamOpenApiConfiguration.class
})
public class PaymentAdministrationVerificationTestConfiguration {
}
