package com.codecore.appointment.configuration;

import com.codecore.appointment.interfaces.http.admin.AppointmentAdminApiPaths;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppointmentOpenApiConfiguration {

    public static final String SCHEDULING_ADMINISTRATION_GROUP = "scheduling-administration";

    @Bean
    public GroupedOpenApi schedulingAdministrationGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group(SCHEDULING_ADMINISTRATION_GROUP)
                .displayName("Scheduling Administration")
                .pathsToMatch(AppointmentAdminApiPaths.BASE + "/**")
                .build();
    }
}
