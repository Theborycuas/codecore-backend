package com.codecore.iam.configuration;

import com.codecore.iam.application.BootstrapPlatformUseCaseImpl;
import com.codecore.iam.application.TenantOperationalGuard;
import com.codecore.iam.application.admin.IdentityRegistrationOrchestrator;
import com.codecore.iam.application.port.in.BootstrapPlatformUseCase;
import com.codecore.iam.application.port.in.CreateTenantUseCase;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.application.port.out.TenantRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Platform bootstrap and tenant operational guard (PASO 15.9.2 / 15.9.3).
 */
@Configuration
@EnableConfigurationProperties(PlatformBootstrapProperties.class)
public class IamBootstrapConfiguration {

    @Bean
    public TenantOperationalGuard tenantOperationalGuard(TenantRepository tenantRepository) {
        return new TenantOperationalGuard(tenantRepository);
    }

    @Bean
    public BootstrapPlatformUseCase bootstrapPlatformUseCase(
            PlatformBootstrapProperties properties,
            TenantRepository tenantRepository,
            CreateTenantUseCase createTenantUseCase,
            IdentityRegistrationOrchestrator identityRegistrationOrchestrator,
            MembershipRepository membershipRepository,
            RoleRepository roleRepository,
            MembershipRoleRepository membershipRoleRepository
    ) {
        return new BootstrapPlatformUseCaseImpl(
                properties,
                tenantRepository,
                createTenantUseCase,
                identityRegistrationOrchestrator,
                membershipRepository,
                roleRepository,
                membershipRoleRepository
        );
    }
}
