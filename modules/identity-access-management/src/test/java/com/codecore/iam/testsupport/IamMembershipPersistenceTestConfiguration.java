package com.codecore.iam.testsupport;

import com.codecore.iam.infrastructure.persistence.mapper.IamIdentityTenantMembershipMapper;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.iam.infrastructure.persistence.repository")
@Import(R2dbcMembershipRepository.class)
public class IamMembershipPersistenceTestConfiguration {

    @Bean
    IamIdentityTenantMembershipMapper iamIdentityTenantMembershipMapper() {
        return new IamIdentityTenantMembershipMapper();
    }
}
