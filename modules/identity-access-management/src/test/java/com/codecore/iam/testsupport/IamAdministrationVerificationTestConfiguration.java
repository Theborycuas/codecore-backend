package com.codecore.iam.testsupport;

import com.codecore.iam.configuration.IamOpenApiConfiguration;
import com.codecore.iam.interfaces.http.admin.IamAdministrationController;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Full IAM administration stack for FASE 15.9 verification (E2E HTTP).
 */
@Configuration
@Import({
        IamUserAdminIntegrationTestConfiguration.class,
        IamAdministrationController.class,
        IamOpenApiConfiguration.class
})
public class IamAdministrationVerificationTestConfiguration {
}
