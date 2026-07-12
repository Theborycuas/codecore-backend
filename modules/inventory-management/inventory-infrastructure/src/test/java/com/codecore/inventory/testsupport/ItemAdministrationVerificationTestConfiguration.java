package com.codecore.inventory.testsupport;

import com.codecore.iam.configuration.IamOpenApiConfiguration;
import com.codecore.inventory.configuration.InventoryOpenApiConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Full Item administration stack for FASE 20.7 verification (E2E HTTP).
 */
@Configuration
@Import({
        ItemAdminIntegrationTestConfiguration.class,
        InventoryOpenApiConfiguration.class,
        IamOpenApiConfiguration.class
})
public class ItemAdministrationVerificationTestConfiguration {
}
