package com.codecore.access.testsupport;

import com.codecore.access.configuration.AccessOpenApiConfiguration;
import com.codecore.iam.configuration.IamOpenApiConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Full Access administration stack for FASE 23.7 verification (E2E HTTP).
 */
@Configuration
@Import({
        AccessAdminIntegrationTestConfiguration.class,
        AccessOpenApiConfiguration.class,
        IamOpenApiConfiguration.class
})
public class AccessAdministrationVerificationTestConfiguration {
}
