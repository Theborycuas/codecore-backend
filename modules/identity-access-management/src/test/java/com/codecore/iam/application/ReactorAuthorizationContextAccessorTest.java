package com.codecore.iam.application;

import com.codecore.iam.application.dto.AuthenticatedPrincipal;
import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.domain.exception.IdentityNotMemberOfTenantException;
import com.codecore.iam.domain.exception.TenantContextUnavailableException;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.interfaces.http.security.AuthorizationReactorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactorAuthorizationContextAccessorTest {

    @Mock
    private MembershipRepository membershipRepository;

    private ReactorAuthorizationContextAccessor accessor;

    private IdentityId identityId;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        accessor = new ReactorAuthorizationContextAccessor(membershipRepository);
        identityId = IdentityId.generate();
        tenantId = TenantId.generate();
    }

    @Test
    void shouldResolveAuthorizationContextFromAuthenticatedPrincipal() {
        IdentityTenantMembership membership = IdentityTenantMembership.create(identityId, tenantId, Instant.now());
        when(membershipRepository.findActiveByIdentityIdAndTenantId(identityId, tenantId))
                .thenReturn(Mono.just(membership));

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                identityId,
                "auth@codecore.local",
                IdentityStatus.ACTIVE,
                Optional.of(tenantId)
        );

        StepVerifier.create(accessor.resolveForPrincipal(principal))
                .assertNext(ctx -> {
                    assertThat(ctx.identityId()).isEqualTo(identityId);
                    assertThat(ctx.tenantId()).isEqualTo(tenantId);
                    assertThat(ctx.membershipId()).isEqualTo(membership.id());
                })
                .verifyComplete();
    }

    @Test
    void shouldFailWhenActiveMembershipMissing() {
        when(membershipRepository.findActiveByIdentityIdAndTenantId(identityId, tenantId))
                .thenReturn(Mono.empty());

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                identityId,
                "auth@codecore.local",
                IdentityStatus.ACTIVE,
                Optional.of(tenantId)
        );

        StepVerifier.create(accessor.resolveForPrincipal(principal))
                .expectError(IdentityNotMemberOfTenantException.class)
                .verify();
    }

    @Test
    void shouldReturnCachedAuthorizationContextFromReactorContext() {
        AuthorizationContext cached = new AuthorizationContext(
                identityId,
                tenantId,
                IdentityTenantMembership.create(identityId, tenantId, Instant.now()).id()
        );

        StepVerifier.create(accessor.current().contextWrite(ctx -> AuthorizationReactorContext.write(ctx, cached)))
                .expectNext(cached)
                .verifyComplete();
    }

    @Test
    void shouldFailWhenTenantClaimAbsent() {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                identityId,
                "auth@codecore.local",
                IdentityStatus.ACTIVE,
                Optional.empty()
        );

        StepVerifier.create(accessor.resolveForPrincipal(principal))
                .expectError(TenantContextUnavailableException.class)
                .verify();
    }

    @Test
    void shouldFailWhenPrincipalMissingFromReactorContext() {
        StepVerifier.create(accessor.current())
                .expectError(TenantContextUnavailableException.class)
                .verify();
    }
}
