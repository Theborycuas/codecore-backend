package com.codecore.access.configuration;

import com.codecore.access.interfaces.http.admin.InvitationAdminApiPaths;
import com.codecore.access.interfaces.http.publicapi.InvitationAcceptApiPaths;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessOpenApiConfiguration {

    public static final String ACCESS_ADMINISTRATION_GROUP = "access-administration";

    @Bean
    public GroupedOpenApi accessAdministrationGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group(ACCESS_ADMINISTRATION_GROUP)
                .displayName("Access Administration")
                .pathsToMatch(
                        InvitationAdminApiPaths.INVITATIONS + "/**",
                        InvitationAcceptApiPaths.ACCEPT
                )
                .build();
    }
}
