package com.codecore.billing.configuration;

import com.codecore.billing.interfaces.http.admin.InvoiceAdminApiPaths;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BillingOpenApiConfiguration {

    public static final String BILLING_ADMINISTRATION_GROUP = "billing-administration";

    @Bean
    public GroupedOpenApi billingAdministrationGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group(BILLING_ADMINISTRATION_GROUP)
                .displayName("Billing Administration")
                .pathsToMatch(InvoiceAdminApiPaths.BASE + "/**")
                .build();
    }
}
