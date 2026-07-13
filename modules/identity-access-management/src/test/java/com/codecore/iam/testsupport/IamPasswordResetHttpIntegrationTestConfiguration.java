package com.codecore.iam.testsupport;

import com.codecore.iam.application.port.out.TenantSystemRolesProvisioner;
import com.codecore.iam.configuration.IamAuthenticationConfiguration;
import com.codecore.iam.configuration.IamBootstrapConfiguration;
import com.codecore.iam.configuration.IamModuleConfiguration;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRoleRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRoleRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcTenantRepository;
import com.codecore.iam.infrastructure.security.BCryptPasswordHasher;
import com.codecore.iam.infrastructure.security.JwtTokenProvider;
import com.codecore.iam.interfaces.http.AuthenticationController;
import com.codecore.iam.interfaces.http.IamHttpExceptionHandler;
import com.codecore.iam.interfaces.http.PasswordResetController;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;

@Configuration
@EnableAutoConfiguration
@Import({
        IamModuleConfiguration.class,
        IamR2dbcTestConfiguration.class,
        IamAuthenticationConfiguration.class,
        IamBootstrapConfiguration.class,
        R2dbcIdentityRepository.class,
        R2dbcMembershipRepository.class,
        R2dbcMembershipRoleRepository.class,
        R2dbcRoleRepository.class,
        R2dbcTenantRepository.class,
        BCryptPasswordHasher.class,
        JwtTokenProvider.class,
        AuthenticationController.class,
        PasswordResetController.class,
        IamHttpExceptionHandler.class
})
public class IamPasswordResetHttpIntegrationTestConfiguration {

    @Bean
    TenantSystemRolesProvisioner noopTenantSystemRolesProvisioner() {
        return tenantId -> Mono.empty();
    }
}
