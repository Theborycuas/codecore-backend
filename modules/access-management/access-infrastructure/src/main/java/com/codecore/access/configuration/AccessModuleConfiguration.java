package com.codecore.access.configuration;

import com.codecore.access.infrastructure.persistence.mapper.InvitationMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Access module Spring entry point — persistence + administration (FASE 23.4 / 23.6).
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.access.infrastructure.persistence.repository")
@Import({AccessAdministrationConfiguration.class, AccessOpenApiConfiguration.class})
public class AccessModuleConfiguration {

    @Bean
    public InvitationMapper invitationMapper() {
        return new InvitationMapper();
    }
}
