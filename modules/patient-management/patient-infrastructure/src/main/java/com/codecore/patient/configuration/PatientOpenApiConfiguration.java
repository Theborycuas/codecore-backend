package com.codecore.patient.configuration;

import com.codecore.patient.interfaces.http.admin.PatientAdminApiPaths;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PatientOpenApiConfiguration {

    public static final String CLINICAL_ADMINISTRATION_GROUP = "clinical-administration";

    @Bean
    public GroupedOpenApi clinicalAdministrationGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group(CLINICAL_ADMINISTRATION_GROUP)
                .displayName("Clinical Administration")
                .pathsToMatch(PatientAdminApiPaths.BASE + "/**")
                .build();
    }
}
