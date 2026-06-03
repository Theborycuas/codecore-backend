package com.codecore.iam.application;

import com.codecore.iam.application.command.RegisterIdentityCommand;
import com.codecore.iam.application.dto.RegisterIdentityResult;
import com.codecore.iam.application.port.in.RegisterIdentityUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.configuration.IamModuleConfiguration;
import com.codecore.iam.domain.exception.IdentityAlreadyExistsException;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcTenantRepository;
import com.codecore.iam.infrastructure.security.BCryptPasswordHasher;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
        IamModuleConfiguration.class,
        R2dbcIdentityRepository.class,
        R2dbcMembershipRepository.class,
        R2dbcTenantRepository.class,
        BCryptPasswordHasher.class
})
class RegisterIdentityUseCaseIT extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";

    @Autowired
    private RegisterIdentityUseCase registerIdentityUseCase;

    @Autowired
    private IdentityRepository identityRepository;

    @Test
    void shouldRegisterIdentityAndPersistRow() throws Exception {
        TenantId tenantId = TenantId.generate();
        String email = "register.%s@codecore.local".formatted(tenantId.value());

        RegisterIdentityCommand command = new RegisterIdentityCommand(tenantId, email, PASSWORD);

        StepVerifier.create(registerIdentityUseCase.execute(command))
                .assertNext(result -> {
                    assertThat(result.tenantId()).isEqualTo(tenantId);
                    assertThat(result.email().value()).isEqualTo(email.toLowerCase());
                    assertThat(result.status()).isEqualTo(IdentityStatus.PENDING_VERIFICATION);
                })
                .verifyComplete();

        try (Connection connection = openJdbcConnection()) {
            assertThat(schemaIamExists(connection)).isTrue();
            assertThat(tableIamUserExists(connection)).isTrue();
            assertThat(flywayHistoryContainsMigrations(connection)).isTrue();

            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT status, email_verified, tenant_id
                    FROM iam.iam_user
                    WHERE tenant_id = ? AND normalized_email = ?
                    """)) {
                ps.setObject(1, tenantId.value());
                ps.setString(2, email.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("status")).isEqualTo("PENDING_VERIFICATION");
                    assertThat(rs.getBoolean("email_verified")).isFalse();
                    assertThat(rs.getObject("tenant_id")).isEqualTo(tenantId.value());
                }
            }

            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT status FROM iam.identity_tenant_membership
                    WHERE tenant_id = ? AND identity_id = (
                        SELECT id FROM iam.iam_user
                        WHERE tenant_id = ? AND normalized_email = ?
                    )
                    """)) {
                ps.setObject(1, tenantId.value());
                ps.setObject(2, tenantId.value());
                ps.setString(3, email.toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("status")).isEqualTo("ACTIVE");
                }
            }
        }
    }

    @Test
    void shouldRejectDuplicateEmailInSameTenant() {
        TenantId tenantId = TenantId.generate();
        String email = "duplicate.%s@codecore.local".formatted(tenantId.value());
        RegisterIdentityCommand command = new RegisterIdentityCommand(tenantId, email, PASSWORD);

        StepVerifier.create(registerIdentityUseCase.execute(command))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(registerIdentityUseCase.execute(command))
                .expectError(IdentityAlreadyExistsException.class)
                .verify();
    }

    @Test
    void shouldAllowSameEmailInDifferentTenants() {
        String email = "shared.%s@codecore.local".formatted(TenantId.generate().value());
        TenantId tenantA = TenantId.generate();
        TenantId tenantB = TenantId.generate();

        StepVerifier.create(registerIdentityUseCase.execute(
                        new RegisterIdentityCommand(tenantA, email, PASSWORD)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(registerIdentityUseCase.execute(
                        new RegisterIdentityCommand(tenantB, email, PASSWORD)))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldRoundTripThroughRepositoryAfterRegistration() {
        TenantId tenantId = TenantId.generate();
        String email = "roundtrip.%s@codecore.local".formatted(tenantId.value());
        RegisterIdentityCommand command = new RegisterIdentityCommand(tenantId, email, PASSWORD);

        StepVerifier.create(
                        registerIdentityUseCase.execute(command)
                                .flatMap(registered -> identityRepository
                                        .findByTenantAndEmail(tenantId, registered.email())
                                        .map(loaded -> new RegisteredAndLoaded(registered, loaded))))
                .assertNext(pair -> {
                    assertThat(pair.registered().identityId()).isEqualTo(pair.loaded().id());
                    assertThat(pair.loaded().tenantId()).isEqualTo(tenantId);
                    assertThat(pair.loaded().email().value()).isEqualTo(email.toLowerCase());
                    assertThat(pair.loaded().status()).isEqualTo(IdentityStatus.PENDING_VERIFICATION);
                    assertThat(pair.loaded().isEmailVerified()).isFalse();
                    assertThat(pair.loaded().credential()).isPresent();
                })
                .verifyComplete();
    }

    private record RegisteredAndLoaded(RegisterIdentityResult registered, Identity loaded) {
    }

    private static boolean schemaIamExists(Connection connection) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM information_schema.schemata WHERE schema_name = 'iam'")) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean tableIamUserExists(Connection connection) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT 1 FROM information_schema.tables
                WHERE table_schema = 'iam' AND table_name = 'iam_user'
                """)) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean flywayHistoryContainsMigrations(Connection connection) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) >= 4;
            }
        }
    }
}
