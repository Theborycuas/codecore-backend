package com.codecore.access.testsupport;

import com.codecore.access.infrastructure.persistence.mapper.InvitationMapper;
import com.codecore.access.infrastructure.persistence.repository.R2dbcInvitationAdminQueryRepository;
import com.codecore.access.infrastructure.persistence.repository.R2dbcInvitationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.access.infrastructure.persistence.repository")
@Import({R2dbcInvitationRepository.class, R2dbcInvitationAdminQueryRepository.class})
public class AccessPersistenceTestConfiguration {

    @Bean
    InvitationMapper invitationMapper() {
        return new InvitationMapper();
    }
}
