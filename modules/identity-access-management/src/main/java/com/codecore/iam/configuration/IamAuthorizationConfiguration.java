package com.codecore.iam.configuration;

import com.codecore.iam.application.AuthorizationServiceImpl;
import com.codecore.iam.application.port.in.AuthorizationService;
import com.codecore.iam.application.port.out.AuthorizationQueryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Authorization evaluation wiring (FASE 14.5).
 */
@Configuration
@EnableAspectJAutoProxy
public class IamAuthorizationConfiguration {

    @Bean
    public AuthorizationService authorizationService(AuthorizationQueryRepository authorizationQueryRepository) {
        return new AuthorizationServiceImpl(authorizationQueryRepository);
    }
}
