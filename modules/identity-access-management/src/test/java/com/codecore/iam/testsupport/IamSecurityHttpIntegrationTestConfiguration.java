package com.codecore.iam.testsupport;

import com.codecore.iam.configuration.IamAuthenticationConfiguration;
import com.codecore.iam.configuration.IamModuleConfiguration;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcTenantRepository;
import com.codecore.iam.infrastructure.security.BCryptPasswordHasher;
import com.codecore.iam.infrastructure.security.JwtTokenProvider;
import com.codecore.iam.infrastructure.security.JwtTokenValidator;
import com.codecore.iam.interfaces.http.AuthenticationController;
import com.codecore.iam.interfaces.http.IamHttpExceptionHandler;
import com.codecore.iam.interfaces.http.security.AuthenticatedPrincipalAuthorizationManager;
import com.codecore.iam.interfaces.http.security.JwtAuthenticationWebFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@Import({
        IamModuleConfiguration.class,
        IamAuthenticationConfiguration.class,
        R2dbcIdentityRepository.class,
        R2dbcTenantRepository.class,
        BCryptPasswordHasher.class,
        JwtTokenProvider.class,
        JwtTokenValidator.class,
        JwtAuthenticationWebFilter.class,
        AuthenticatedPrincipalAuthorizationManager.class,
        AuthenticationController.class,
        IamHttpExceptionHandler.class
})
public class IamSecurityHttpIntegrationTestConfiguration {
}
