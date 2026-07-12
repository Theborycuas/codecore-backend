package com.codecore.encounter.configuration;

import com.codecore.encounter.interfaces.http.admin.EncounterAdminApiPaths;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncounterOpenApiConfiguration {

    public static final String RECORDS_ADMINISTRATION_GROUP = "records-administration";

    @Bean
    public GroupedOpenApi recordsAdministrationGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group(RECORDS_ADMINISTRATION_GROUP)
                .displayName("Records Administration")
                .pathsToMatch(EncounterAdminApiPaths.BASE + "/**")
                .build();
    }
}
