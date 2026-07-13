package com.codecore.audit.configuration;

import com.codecore.audit.interfaces.http.admin.AuditAdminApiPaths;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditOpenApiConfiguration {

    public static final String AUDIT_ADMINISTRATION_GROUP = "audit-administration";

    @Bean
    public GroupedOpenApi auditAdministrationGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group(AUDIT_ADMINISTRATION_GROUP)
                .displayName("Audit Administration")
                .pathsToMatch(AuditAdminApiPaths.ENTRIES + "/**")
                .build();
    }
}
