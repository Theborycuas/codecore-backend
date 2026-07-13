package com.codecore.audit.configuration;

import com.codecore.audit.infrastructure.persistence.mapper.AuditEntryMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Audit module Spring entry point — persistence + administration (FASE 24.4 / 24.6).
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.audit.infrastructure.persistence.repository")
@Import({AuditAdministrationConfiguration.class, AuditOpenApiConfiguration.class})
public class AuditModuleConfiguration {

    @Bean
    public AuditEntryMapper auditEntryMapper() {
        return new AuditEntryMapper();
    }
}
