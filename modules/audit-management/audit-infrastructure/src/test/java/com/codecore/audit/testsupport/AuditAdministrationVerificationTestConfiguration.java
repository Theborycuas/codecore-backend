package com.codecore.audit.testsupport;

import com.codecore.audit.configuration.AuditOpenApiConfiguration;
import com.codecore.iam.configuration.IamOpenApiConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Full Audit administration stack for FASE 24.7 verification (E2E HTTP).
 */
@Configuration
@Import({
        AuditAdminIntegrationTestConfiguration.class,
        AuditOpenApiConfiguration.class,
        IamOpenApiConfiguration.class
})
public class AuditAdministrationVerificationTestConfiguration {
}
