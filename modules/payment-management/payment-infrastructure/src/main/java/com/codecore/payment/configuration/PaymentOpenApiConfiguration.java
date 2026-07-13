package com.codecore.payment.configuration;

import com.codecore.payment.interfaces.http.admin.PaymentAdminApiPaths;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentOpenApiConfiguration {

    public static final String PAYMENTS_ADMINISTRATION_GROUP = "payments-administration";

    @Bean
    public GroupedOpenApi paymentsAdministrationGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group(PAYMENTS_ADMINISTRATION_GROUP)
                .displayName("Payments Administration")
                .pathsToMatch(PaymentAdminApiPaths.PAYMENTS + "/**")
                .build();
    }
}
