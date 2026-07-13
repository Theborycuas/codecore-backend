package com.codecore.audit.testsupport;

import com.codecore.audit.infrastructure.persistence.mapper.AuditEntryMapper;
import com.codecore.audit.infrastructure.persistence.repository.R2dbcAuditAdminQueryRepository;
import com.codecore.audit.infrastructure.persistence.repository.R2dbcAuditEntryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.audit.infrastructure.persistence.repository")
@Import({R2dbcAuditEntryRepository.class, R2dbcAuditAdminQueryRepository.class})
public class AuditPersistenceTestConfiguration {

    @Bean
    AuditEntryMapper auditEntryMapper() {
        return new AuditEntryMapper();
    }
}
