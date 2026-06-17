package com.codecore.iam.configuration;

import com.codecore.iam.interfaces.http.admin.IamAdminApiPaths;
import com.codecore.iam.interfaces.http.openapi.RequiresPermissionOperationCustomizer;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI contract for IAM administration HTTP API (FASE 15.8, ADR-008).
 */
@Configuration
public class IamOpenApiConfiguration {

    public static final String IAM_ADMINISTRATION_GROUP = "iam-administration";

    public static final String BEARER_JWT_SCHEME = "bearer-jwt";

    public static final String TENANT_HEADER_SCHEME = "tenant-header";

    @Bean
    public OpenAPI iamAdministrationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CodeCore IAM Administration API")
                        .version("1.0.0")
                        .description("""
                                Administrative HTTP surface for Identity, Membership, Role, Permission, \
                                and Tenant resources under `/api/v1/iam/**`.

                                All operations require a valid JWT (`Authorization: Bearer <token>`) except \
                                where noted. Tenant scope is resolved from the JWT `tenantId` claim; \
                                `X-Tenant-Id` is required on login and may be sent on other routes.

                                Permission codes are enforced at runtime via `@RequiresPermission` and \
                                documented per operation as extension `x-permission` (ADR-007).
                                """)
                        .contact(new Contact().name("CodeCore").url("https://codecore.local")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_JWT_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token from `POST /api/v1/auth/login`"))
                        .addSecuritySchemes(TENANT_HEADER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Tenant-Id")
                                .description("Tenant UUID; required on login, optional when JWT embeds tenantId")));
    }

    @Bean
    public GroupedOpenApi iamAdministrationGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group(IAM_ADMINISTRATION_GROUP)
                .displayName("IAM Administration")
                .pathsToMatch(IamAdminApiPaths.BASE + "/**")
                .build();
    }

    @Bean
    public GlobalOperationCustomizer requiresPermissionOperationCustomizer() {
        return new RequiresPermissionOperationCustomizer();
    }
}
