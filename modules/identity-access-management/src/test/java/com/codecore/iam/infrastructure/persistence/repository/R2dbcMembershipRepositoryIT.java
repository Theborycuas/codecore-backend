package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamMembershipPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(IamMembershipPersistenceTestConfiguration.class)
class R2dbcMembershipRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private MembershipRepository membershipRepository;

    @Test
    void shouldPersistAndFindMembershipsForIdentityAcrossTenants() {
        IdentityId identityId = IdentityId.generate();
        TenantId tenantA = TenantId.generate();
        TenantId tenantB = TenantId.generate();
        Instant now = Instant.now();

        IdentityTenantMembership membershipA = IdentityTenantMembership.create(identityId, tenantA, now);
        IdentityTenantMembership membershipB = IdentityTenantMembership.create(identityId, tenantB, now);

        StepVerifier.create(membershipRepository.save(membershipA))
                .assertNext(saved -> assertThat(saved.status()).isEqualTo(MembershipStatus.ACTIVE))
                .verifyComplete();

        StepVerifier.create(membershipRepository.save(membershipB))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(membershipRepository.exists(identityId, tenantA))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(membershipRepository.exists(identityId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(membershipRepository.findByIdentityId(identityId).collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list).extracting(m -> m.tenantId().value())
                            .containsExactlyInAnyOrder(tenantA.value(), tenantB.value());
                })
                .verifyComplete();
    }

    @Test
    void shouldFindMembershipsByTenantId() {
        TenantId tenantId = TenantId.generate();
        IdentityId identityOne = IdentityId.generate();
        IdentityId identityTwo = IdentityId.generate();
        Instant now = Instant.now();

        StepVerifier.create(membershipRepository.save(
                        IdentityTenantMembership.create(identityOne, tenantId, now)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(membershipRepository.save(
                        IdentityTenantMembership.create(identityTwo, tenantId, now)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(membershipRepository.findByTenantId(tenantId).collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list).extracting(m -> m.identityId().value())
                            .containsExactlyInAnyOrder(identityOne.value(), identityTwo.value());
                })
                .verifyComplete();
    }

    @Test
    void shouldUpdateMembershipStatus() {
        IdentityId identityId = IdentityId.generate();
        TenantId tenantId = TenantId.generate();
        IdentityTenantMembership membership = IdentityTenantMembership.create(identityId, tenantId, Instant.now());

        StepVerifier.create(membershipRepository.save(membership))
                .expectNextCount(1)
                .verifyComplete();

        membership.deactivate();
        StepVerifier.create(membershipRepository.save(membership))
                .assertNext(saved -> assertThat(saved.status()).isEqualTo(MembershipStatus.INACTIVE))
                .verifyComplete();

        StepVerifier.create(membershipRepository.findByIdentityId(identityId))
                .assertNext(found -> assertThat(found.status()).isEqualTo(MembershipStatus.INACTIVE))
                .verifyComplete();
    }
}
