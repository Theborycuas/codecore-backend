package com.codecore.access.infrastructure.adapters;

import com.codecore.access.application.port.out.InvitationRepository;
import com.codecore.access.contract.reference.InvitationReferencePort;
import com.codecore.access.domain.model.invitation.Invitation;
import com.codecore.access.domain.valueobject.EmailAddress;
import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.domain.valueobject.InvitationRoleCode;
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

@DataR2dbcTest
@Import({
        AccessPersistenceTestConfiguration.class,
        R2dbcInvitationReferenceAdapter.class
})
class InvitationReferencePortIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-12T21:00:00Z");

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private InvitationReferencePort invitationReferencePort;

    @Test
    void shouldReturnTrueForPendingInvitationInTenant() {
        InvitationId invitationId = InvitationId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(invitationRepository.save(pendingInvitation(invitationId, tenantId)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invitationReferencePort.existsPendingByIdAndTenant(invitationId, tenantId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseForWrongTenantOrUnknownId() {
        InvitationId invitationId = InvitationId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(invitationRepository.save(pendingInvitation(invitationId, tenantId)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invitationReferencePort.existsPendingByIdAndTenant(invitationId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(invitationReferencePort.existsPendingByIdAndTenant(InvitationId.generate(), tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenInvitationIsRevoked() {
        InvitationId invitationId = InvitationId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(invitationRepository.save(pendingInvitation(invitationId, tenantId)).flatMap(saved -> {
                    saved.revoke(NOW.plusSeconds(10));
                    return invitationRepository.save(saved);
                }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invitationReferencePort.existsPendingByIdAndTenant(invitationId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    private static Invitation pendingInvitation(InvitationId invitationId, TenantId tenantId) {
        return Invitation.create(
                invitationId,
                tenantId,
                EmailAddress.of("ref@example.com"),
                InvitationRoleCode.of("MANAGER"),
                MembershipId.of(UUID.randomUUID()),
                InvitationTokenHash.ofHashedValue("ref-hash-" + invitationId.value()),
                NOW.plusSeconds(3600),
                NOW
        );
    }
}
