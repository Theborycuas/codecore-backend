package com.codecore.iam.configuration;

import com.codecore.iam.application.AuthenticateIdentityUseCaseImpl;
import com.codecore.iam.application.port.in.AuthenticateIdentityUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.application.port.out.TokenProvider;
import com.codecore.iam.infrastructure.security.config.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Authentication + JWT wiring (loaded by API runtime and authentication integration tests).
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class IamAuthenticationConfiguration {

    @Bean
    public AuthenticateIdentityUseCase authenticateIdentityUseCase(
            IdentityRepository identityRepository,
            MembershipRepository membershipRepository,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider
    ) {
        return new AuthenticateIdentityUseCaseImpl(
                identityRepository,
                membershipRepository,
                passwordHasher,
                tokenProvider
        );
    }
}
