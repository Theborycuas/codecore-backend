package com.codecore.organization.testsupport;

import com.codecore.iam.configuration.IamOpenApiConfiguration;
import com.codecore.organization.configuration.OrgOpenApiConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Full Organization administration stack for FASE 16.9 verification (E2E HTTP).
 */
@Configuration
@Import({
        OrgAdminIntegrationTestConfiguration.class,
        OrgOpenApiConfiguration.class,
        IamOpenApiConfiguration.class
})
public class OrganizationAdministrationVerificationTestConfiguration {
}
