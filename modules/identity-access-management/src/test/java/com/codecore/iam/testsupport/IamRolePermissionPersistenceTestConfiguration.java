package com.codecore.iam.testsupport;

import com.codecore.iam.infrastructure.persistence.mapper.IamPermissionMapper;
import com.codecore.iam.infrastructure.persistence.mapper.IamRoleMapper;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcPermissionRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRolePermissionRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRoleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.iam.infrastructure.persistence.repository")
@Import({R2dbcRoleRepository.class, R2dbcPermissionRepository.class, R2dbcRolePermissionRepository.class})
public class IamRolePermissionPersistenceTestConfiguration {

    @Bean
    IamRoleMapper iamRoleMapper() {
        return new IamRoleMapper();
    }

    @Bean
    IamPermissionMapper iamPermissionMapper() {
        return new IamPermissionMapper();
    }
}
