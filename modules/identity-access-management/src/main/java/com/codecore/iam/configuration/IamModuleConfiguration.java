package com.codecore.iam.configuration;

import com.codecore.iam.application.CreateTenantUseCaseImpl;
import com.codecore.iam.application.RegisterIdentityUseCaseImpl;
import com.codecore.iam.application.port.in.CreateTenantUseCase;
import com.codecore.iam.application.port.in.RegisterIdentityUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.infrastructure.persistence.mapper.IamIdentityTenantMembershipMapper;
import com.codecore.iam.infrastructure.persistence.mapper.IamTenantMapper;
import com.codecore.iam.infrastructure.persistence.mapper.IamUserMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * IAM module Spring entry point. Registers persistence adapters and registration use case.
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.iam.infrastructure.persistence.repository")
public class IamModuleConfiguration {

    @Bean
    public IamUserMapper iamUserMapper() {
        return new IamUserMapper();
    }

    @Bean
    public IamTenantMapper iamTenantMapper() {
        return new IamTenantMapper();
    }

    @Bean
    public IamIdentityTenantMembershipMapper iamIdentityTenantMembershipMapper() {
        return new IamIdentityTenantMembershipMapper();
    }

    @Bean
    public RegisterIdentityUseCase registerIdentityUseCase(
            IdentityRepository identityRepository,
            MembershipRepository membershipRepository,
            PasswordHasher passwordHasher
    ) {
        return new RegisterIdentityUseCaseImpl(identityRepository, membershipRepository, passwordHasher);
    }

    @Bean
    public CreateTenantUseCase createTenantUseCase(TenantRepository tenantRepository) {
        return new CreateTenantUseCaseImpl(tenantRepository);
    }
}
