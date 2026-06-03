package com.codecore.iam.testsupport;

import com.codecore.iam.application.CreateTenantUseCaseImpl;
import com.codecore.iam.application.port.in.CreateTenantUseCase;
import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.infrastructure.persistence.mapper.IamTenantMapper;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcTenantRepository;
import com.codecore.iam.interfaces.http.CreateTenantController;
import com.codecore.iam.interfaces.http.IamHttpExceptionHandler;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableAutoConfiguration
@EnableR2dbcRepositories(basePackages = "com.codecore.iam.infrastructure.persistence.repository")
@Import({
        R2dbcTenantRepository.class,
        CreateTenantController.class,
        IamHttpExceptionHandler.class
})
public class IamTenantHttpIntegrationTestConfiguration {

    @Bean
    IamTenantMapper iamTenantMapper() {
        return new IamTenantMapper();
    }

    @Bean
    CreateTenantUseCase createTenantUseCase(TenantRepository tenantRepository) {
        return new CreateTenantUseCaseImpl(tenantRepository);
    }
}
