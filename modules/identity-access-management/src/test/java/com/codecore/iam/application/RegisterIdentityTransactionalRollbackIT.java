package com.codecore.iam.application;

import com.codecore.iam.application.command.RegisterIdentityCommand;
import com.codecore.iam.application.port.in.RegisterIdentityUseCase;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.configuration.IamModuleConfiguration;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcTenantRepository;
import com.codecore.iam.infrastructure.security.BCryptPasswordHasher;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamR2dbcTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
        IamModuleConfiguration.class,
        IamR2dbcTestConfiguration.class,
        R2dbcIdentityRepository.class,
        R2dbcMembershipRepository.class,
        R2dbcTenantRepository.class,
        BCryptPasswordHasher.class,
        RegisterIdentityTransactionalRollbackIT.FailingMembershipConfiguration.class
})
class RegisterIdentityTransactionalRollbackIT extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";

    @Autowired
    private RegisterIdentityUseCase registerIdentityUseCase;

    @Test
    void shouldRollbackIdentityWhenMembershipSaveFails() throws Exception {
        TenantId tenantId = TenantId.generate();
        String email = "rollback.%s@codecore.local".formatted(tenantId.value());

        StepVerifier.create(registerIdentityUseCase.execute(
                        new RegisterIdentityCommand(tenantId, email, PASSWORD)))
                .expectError(IllegalStateException.class)
                .verify();

        try (Connection connection = openJdbcConnection()) {
            assertThat(countIamUserRows(connection, tenantId, email)).isZero();
            assertThat(countMembershipRows(connection, tenantId, email)).isZero();
        }
    }

    private static int countIamUserRows(Connection connection, TenantId tenantId, String email)
            throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT COUNT(*) FROM iam.iam_user
                WHERE tenant_id = ? AND normalized_email = ?
                """)) {
            ps.setObject(1, tenantId.value());
            ps.setString(2, email.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static int countMembershipRows(Connection connection, TenantId tenantId, String email)
            throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT COUNT(*) FROM iam.identity_tenant_membership m
                WHERE m.tenant_id = ?
                  AND m.identity_id = (
                      SELECT id FROM iam.iam_user
                      WHERE tenant_id = ? AND normalized_email = ?
                  )
                """)) {
            ps.setObject(1, tenantId.value());
            ps.setObject(2, tenantId.value());
            ps.setString(3, email.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @TestConfiguration
    static class FailingMembershipConfiguration {

        @Bean
        @Primary
        MembershipRepository failingMembershipRepository(R2dbcMembershipRepository r2dbcMembershipRepository) {
            return new MembershipRepository() {
                @Override
                public Mono<IdentityTenantMembership> save(IdentityTenantMembership membership) {
                    return Mono.error(new IllegalStateException("Simulated membership persistence failure"));
                }

                @Override
                public Mono<Boolean> exists(IdentityId identityId, TenantId tenantId) {
                    return r2dbcMembershipRepository.exists(identityId, tenantId);
                }

                @Override
                public Flux<IdentityTenantMembership> findByIdentityId(IdentityId identityId) {
                    return r2dbcMembershipRepository.findByIdentityId(identityId);
                }

                @Override
                public Flux<IdentityTenantMembership> findByTenantId(TenantId tenantId) {
                    return r2dbcMembershipRepository.findByTenantId(tenantId);
                }

                @Override
                public Mono<IdentityTenantMembership> findActiveByIdentityIdAndTenantId(
                        IdentityId identityId,
                        TenantId tenantId
                ) {
                    return r2dbcMembershipRepository.findActiveByIdentityIdAndTenantId(identityId, tenantId);
                }
            };
        }
    }
}
