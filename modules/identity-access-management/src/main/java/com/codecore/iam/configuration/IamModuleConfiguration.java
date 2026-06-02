package com.codecore.iam.configuration;

import com.codecore.iam.infrastructure.persistence.mapper.IamUserMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * IAM module Spring entry point. Registers outbound persistence adapters only.
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.iam.infrastructure.persistence.repository")
public class IamModuleConfiguration {

    @Bean
    public IamUserMapper iamUserMapper() {
        return new IamUserMapper();
    }
}
