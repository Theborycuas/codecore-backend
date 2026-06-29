package com.codecore.organization.configuration;

import com.codecore.organization.interfaces.http.admin.OrgAdminApiPaths;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI grouped contract for Organization Management (FASE 16.10).
 * <p>
 * Global {@link io.swagger.v3.oas.models.OpenAPI} and {@code x-permission} customizer
 * are provided by IAM {@code IamOpenApiConfiguration} in the assembled API.
 */
@Configuration
public class OrgOpenApiConfiguration {

    public static final String ORG_ADMINISTRATION_GROUP = "org-administration";

    @Bean
    public GroupedOpenApi orgAdministrationGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group(ORG_ADMINISTRATION_GROUP)
                .displayName("Organization Administration")
                .pathsToMatch(OrgAdminApiPaths.BASE + "/**")
                .build();
    }
}
