package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.configuration.IamModuleConfiguration;
import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.valueobject.CredentialId;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcTenantRepository;
import com.codecore.iam.infrastructure.security.BCryptPasswordHasher;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({IamModuleConfiguration.class, R2dbcIdentityRepository.class, R2dbcTenantRepository.class, BCryptPasswordHasher.class})
class R2dbcIdentityRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private IdentityRepository identityRepository;

    @Test
    void shouldPersistFindAndDeleteIdentityByTenantAndEmail() {
        TenantId tenantId = TenantId.generate();
        IdentityId identityId = IdentityId.generate();
        String email = "persist.test.%s@codecore.local".formatted(identityId.value());
        Identity identity = newIdentity(tenantId, email, identityId, IdentityStatus.ACTIVE);

        StepVerifier.create(identityRepository.save(identity))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(identity.id());
                    assertThat(saved.tenantId()).isEqualTo(tenantId);
                    assertThat(saved.email().value()).isEqualTo(email);
                    assertThat(saved.status()).isEqualTo(IdentityStatus.ACTIVE);
                    assertThat(saved.isEmailVerified()).isTrue();
                    assertThat(saved.version()).isZero();
                    assertThat(saved.credential()).isPresent();
                })
                .verifyComplete();

        StepVerifier.create(identityRepository.existsByTenantAndEmail(tenantId, EmailAddress.of(email)))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(identityRepository.findByTenantAndEmail(tenantId, EmailAddress.of(email)))
                .assertNext(found -> {
                    assertThat(found.id()).isEqualTo(identity.id());
                    assertThat(found.status()).isEqualTo(IdentityStatus.ACTIVE);
                    assertThat(found.email().value()).isEqualTo(email);
                })
                .verifyComplete();

        StepVerifier.create(identityRepository.findById(tenantId, identity.id()))
                .assertNext(found -> assertThat(found.email().value()).isEqualTo(email))
                .verifyComplete();

        StepVerifier.create(identityRepository.delete(tenantId, identity.id()))
                .verifyComplete();

        StepVerifier.create(identityRepository.findById(tenantId, identity.id()))
                .verifyComplete();

        StepVerifier.create(identityRepository.existsByTenantAndEmail(tenantId, EmailAddress.of(email)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldRoundTripPendingVerificationStatusAndEmailVerifiedProjection() {
        TenantId tenantId = TenantId.generate();
        IdentityId identityId = IdentityId.generate();
        String email = "pending.%s@codecore.local".formatted(identityId.value());
        Identity identity = newIdentity(tenantId, email, identityId, IdentityStatus.PENDING_VERIFICATION);

        assertThat(identity.isEmailVerified()).isFalse();

        StepVerifier.create(identityRepository.save(identity).flatMap(identityRepository::save))
                .assertNext(reloaded -> {
                    assertThat(reloaded.status()).isEqualTo(IdentityStatus.PENDING_VERIFICATION);
                    assertThat(reloaded.isEmailVerified()).isFalse();
                    assertThat(reloaded.email().value()).isEqualTo(email);
                })
                .verifyComplete();
    }

    @Test
    void shouldEnforceUniqueNormalizedEmailPerTenant() {
        TenantId tenantId = TenantId.generate();
        String email = "duplicate.%s@codecore.local".formatted(IdentityId.generate().value());

        Identity first = newIdentity(tenantId, email, IdentityId.generate(), IdentityStatus.ACTIVE);
        Identity second = newIdentity(tenantId, email, IdentityId.generate(), IdentityStatus.ACTIVE);

        StepVerifier.create(identityRepository.save(first))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(identityRepository.save(second))
                .expectError()
                .verify();
    }

    private static Identity newIdentity(
            TenantId tenantId,
            String email,
            IdentityId id,
            IdentityStatus status
    ) {
        EmailAddress emailAddress = EmailAddress.of(email);
        Instant now = Instant.now();
        Credential credential = new Credential(
                new CredentialId(id.value()),
                PasswordHash.ofHashedValue("$2a$10$fakehashforintegrationtests"),
                now,
                null,
                status == IdentityStatus.PASSWORD_RESET_REQUIRED,
                0L
        );
        return new Identity(
                id,
                tenantId,
                emailAddress,
                status,
                credential,
                null,
                now,
                now,
                0L
        );
    }
}
