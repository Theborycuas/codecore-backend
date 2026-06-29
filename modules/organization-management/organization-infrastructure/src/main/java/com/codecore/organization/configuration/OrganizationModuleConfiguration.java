package com.codecore.organization.configuration;

import com.codecore.organization.infrastructure.persistence.mapper.OfficeMapper;
import com.codecore.organization.infrastructure.persistence.mapper.OrganizationMapper;
import com.codecore.organization.infrastructure.persistence.mapper.StaffAssignmentMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Organization Management module Spring entry point — persistence adapters (FASE 16.2).
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.organization.infrastructure.persistence.repository")
@Import({OrganizationAdministrationConfiguration.class, OrgOpenApiConfiguration.class})
public class OrganizationModuleConfiguration {

    @Bean
    public OrganizationMapper organizationMapper() {
        return new OrganizationMapper();
    }

    @Bean
    public OfficeMapper officeMapper() {
        return new OfficeMapper();
    }

    @Bean
    public StaffAssignmentMapper staffAssignmentMapper() {
        return new StaffAssignmentMapper();
    }
}
