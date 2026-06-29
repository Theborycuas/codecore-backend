package com.codecore.organization.testsupport;

import com.codecore.organization.infrastructure.persistence.mapper.StaffAssignmentMapper;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcStaffAssignmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.organization.infrastructure.persistence.repository")
@Import(R2dbcStaffAssignmentRepository.class)
public class StaffAssignmentPersistenceTestConfiguration {

    @Bean
    StaffAssignmentMapper staffAssignmentMapper() {
        return new StaffAssignmentMapper();
    }
}
