package com.codecore.organization.testsupport;

import com.codecore.organization.infrastructure.persistence.mapper.OrganizationMapper;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcOrganizationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.organization.infrastructure.persistence.repository")
@Import(R2dbcOrganizationRepository.class)
public class OrganizationPersistenceTestConfiguration {

    @Bean
    OrganizationMapper organizationMapper() {
        return new OrganizationMapper();
    }
}
