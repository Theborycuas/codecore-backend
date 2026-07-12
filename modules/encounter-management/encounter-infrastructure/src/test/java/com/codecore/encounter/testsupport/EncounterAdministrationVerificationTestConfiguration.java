package com.codecore.encounter.testsupport;

import com.codecore.encounter.configuration.EncounterOpenApiConfiguration;
import com.codecore.iam.configuration.IamOpenApiConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Full Encounter administration stack for FASE 19.7 verification (E2E HTTP).
 */
@Configuration
@Import({
        EncounterAdminIntegrationTestConfiguration.class,
        EncounterOpenApiConfiguration.class,
        IamOpenApiConfiguration.class
})
public class EncounterAdministrationVerificationTestConfiguration {
}
