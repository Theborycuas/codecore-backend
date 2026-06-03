package com.codecore.iam.testsupport;

import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Test helper to persist identity + active membership (login integration tests).
 */
public final class IamTestMembershipSupport {

    private IamTestMembershipSupport() {
    }

    public static Mono<Identity> saveIdentityWithActiveMembership(
            IdentityRepository identityRepository,
            MembershipRepository membershipRepository,
            Identity identity
    ) {
        return identityRepository.save(identity)
                .flatMap(saved -> {
                    IdentityTenantMembership membership = IdentityTenantMembership.create(
                            saved.id(),
                            saved.tenantId(),
                            Instant.now()
                    );
                    return membershipRepository.save(membership).thenReturn(saved);
                });
    }
}
