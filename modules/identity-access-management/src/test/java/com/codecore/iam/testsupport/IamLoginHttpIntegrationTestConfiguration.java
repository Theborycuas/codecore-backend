package com.codecore.iam.testsupport;

import com.codecore.iam.configuration.IamAuthenticationConfiguration;
import com.codecore.iam.configuration.IamModuleConfiguration;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcTenantRepository;
import com.codecore.iam.infrastructure.security.BCryptPasswordHasher;
import com.codecore.iam.infrastructure.security.JwtTokenProvider;
import com.codecore.iam.interfaces.http.AuthenticationController;
import com.codecore.iam.interfaces.http.IamHttpExceptionHandler;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@Import({
        IamModuleConfiguration.class,
        IamR2dbcTestConfiguration.class,
        IamAuthenticationConfiguration.class,
        R2dbcIdentityRepository.class,
        R2dbcMembershipRepository.class,
        R2dbcTenantRepository.class,
        BCryptPasswordHasher.class,
        JwtTokenProvider.class,
        AuthenticationController.class,
        IamHttpExceptionHandler.class
})
public class IamLoginHttpIntegrationTestConfiguration {
}
