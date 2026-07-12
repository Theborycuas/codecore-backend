package com.codecore.patient.testsupport;

import com.codecore.iam.configuration.IamOpenApiConfiguration;
import com.codecore.patient.configuration.PatientOpenApiConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Full Patient administration stack for FASE 17.7 verification (E2E HTTP).
 */
@Configuration
@Import({
        PatientAdminIntegrationTestConfiguration.class,
        PatientOpenApiConfiguration.class,
        IamOpenApiConfiguration.class
})
public class PatientAdministrationVerificationTestConfiguration {
}
