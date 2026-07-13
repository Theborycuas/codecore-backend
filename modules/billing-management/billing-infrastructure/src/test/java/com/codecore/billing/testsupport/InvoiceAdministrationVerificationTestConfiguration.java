package com.codecore.billing.testsupport;

import com.codecore.billing.configuration.BillingOpenApiConfiguration;
import com.codecore.iam.configuration.IamOpenApiConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Full Invoice administration stack for FASE 21.7 verification (E2E HTTP).
 */
@Configuration
@Import({
        InvoiceAdminIntegrationTestConfiguration.class,
        BillingOpenApiConfiguration.class,
        IamOpenApiConfiguration.class
})
public class InvoiceAdministrationVerificationTestConfiguration {
}
