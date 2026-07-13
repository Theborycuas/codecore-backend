package com.codecore.access.infrastructure.persistence.repository;

import com.codecore.access.application.port.out.InvitationQueryPort;
import com.codecore.access.application.port.out.InvitationRepository;
import com.codecore.access.domain.model.invitation.Invitation;
import com.codecore.access.domain.valueobject.EmailAddress;
import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.domain.valueobject.InvitationRoleCode;
import com.codecore.access.domain.valueobject.InvitationStatus;
import com.codecore.access.domain.valueobject.InvitationTokenHash;
import com.codecore.access.domain.valueobject.MembershipId;
import com.codecore.access.domain.valueobject.TenantId;
import com.codecore.access.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.access.testsupport.AccessPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(AccessPersistenceTestConfiguration.class)
class R2dbcInvitationRepositoryIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-12T19:00:00Z");

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private InvitationQueryPort invitationQueryPort;

    @Test
    void shouldPersistAndFindById() {
        InvitationId invitationId = InvitationId.generate();
        TenantId tenantId = TenantId.generate();
        Invitation invitation = pendingInvitation(invitationId, tenantId, "hash-" + invitationId.value());

        StepVerifier.create(invitationRepository.save(invitation))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(invitationId);
                    assertThat(saved.tenantId()).isEqualTo(tenantId);
                    assertThat(saved.status()).isEqualTo(InvitationStatus.PENDING);
                    assertThat(saved.invitedEmail().value()).isEqualTo("invitee@example.com");
                })
                .verifyComplete();

        StepVerifier.create(invitationRepository.findById(invitationId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(invitationId))
                .verifyComplete();
    }

    @Test
    void shouldPersistRevokeLifecycle() {
        InvitationId invitationId = InvitationId.generate();
        TenantId tenantId = TenantId.generate();
        Invitation invitation = pendingInvitation(invitationId, tenantId, "hash-revoke-" + invitationId.value());

        StepVerifier.create(invitationRepository.save(invitation)
                        .flatMap(saved -> {
                            saved.revoke(NOW.plusSeconds(60));
                            return invitationRepository.save(saved);
                        }))
                .assertNext(revoked -> assertThat(revoked.status()).isEqualTo(InvitationStatus.REVOKED))
                .verifyComplete();
    }

    @Test
    void shouldFindByTokenHashAndDetectPendingEmail() {
        InvitationId invitationId = InvitationId.generate();
        TenantId tenantId = TenantId.generate();
        String tokenHash = "token-hash-" + invitationId.value();
        Invitation invitation = pendingInvitation(invitationId, tenantId, tokenHash);

        StepVerifier.create(invitationRepository.save(invitation))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invitationQueryPort.findByTokenHash(InvitationTokenHash.ofHashedValue(tokenHash)))
                .assertNext(found -> assertThat(found.id()).isEqualTo(invitationId))
                .verifyComplete();

        StepVerifier.create(invitationQueryPort.existsPendingByEmailAndTenant(
                        EmailAddress.of("invitee@example.com"), tenantId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(invitationQueryPort.findByIdAndTenantId(invitationId, TenantId.generate()))
                .verifyComplete();
    }

    private static Invitation pendingInvitation(InvitationId invitationId, TenantId tenantId, String tokenHash) {
        return Invitation.create(
                invitationId,
                tenantId,
                EmailAddress.of("invitee@example.com"),
                InvitationRoleCode.of("USER"),
                MembershipId.of(UUID.randomUUID()),
                InvitationTokenHash.ofHashedValue(tokenHash),
                NOW.plusSeconds(3600),
                NOW
        );
    }
}
