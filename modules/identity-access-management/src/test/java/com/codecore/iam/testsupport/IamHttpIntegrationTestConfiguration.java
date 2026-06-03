package com.codecore.iam.testsupport;

import com.codecore.iam.configuration.IamModuleConfiguration;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcTenantRepository;
import com.codecore.iam.infrastructure.security.BCryptPasswordHasher;
import com.codecore.iam.interfaces.http.IamHttpExceptionHandler;
import com.codecore.iam.interfaces.http.RegisterIdentityController;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@Import({
        IamModuleConfiguration.class,
        R2dbcIdentityRepository.class,
        R2dbcMembershipRepository.class,
        R2dbcTenantRepository.class,
        BCryptPasswordHasher.class,
        RegisterIdentityController.class,
        IamHttpExceptionHandler.class
})
public class IamHttpIntegrationTestConfiguration {
}
