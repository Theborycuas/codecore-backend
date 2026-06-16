package com.codecore.iam.application;

import com.codecore.iam.application.dto.AuthenticatedPrincipal;
import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.domain.exception.IdentityNotMemberOfTenantException;
import com.codecore.iam.domain.exception.TenantContextUnavailableException;
import com.codecore.iam.interfaces.http.security.AuthenticationContext;
import com.codecore.iam.interfaces.http.security.AuthorizationReactorContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Resolves {@link AuthorizationContext} from JWT principal + active membership in tenant.
 */
@Component
public class ReactorAuthorizationContextAccessor implements AuthorizationContextAccessor {

    private static final String NOT_AUTHENTICATED_MESSAGE =
            "Authenticated principal is not available for the current request";
    private static final String TENANT_CLAIM_ABSENT_MESSAGE =
            "Tenant claim is not present in the authenticated JWT";
    private static final String NOT_MEMBER_MESSAGE =
            "Identity is not an active member of this tenant";

    private final MembershipRepository membershipRepository;

    public ReactorAuthorizationContextAccessor(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public Mono<AuthorizationContext> current() {
        return Mono.deferContextual(ctx -> AuthorizationReactorContext.get(ctx)
                .map(Mono::just)
                .orElseGet(this::resolveFromAuthentication));
    }

    private Mono<AuthorizationContext> resolveFromAuthentication() {
        return AuthenticationContext.currentPrincipal()
                .switchIfEmpty(Mono.error(new TenantContextUnavailableException(
                        TenantContextUnavailableException.Reason.NOT_AUTHENTICATED,
                        NOT_AUTHENTICATED_MESSAGE
                )))
                .flatMap(this::resolveForPrincipal);
    }

    public Mono<AuthorizationContext> resolveForPrincipal(AuthenticatedPrincipal principal) {
        return principal.tenantId()
                .map(tenantId -> membershipRepository.findActiveByIdentityIdAndTenantId(
                                principal.identityId(),
                                tenantId
                        )
                        .switchIfEmpty(Mono.error(new IdentityNotMemberOfTenantException(NOT_MEMBER_MESSAGE)))
                        .map(membership -> new AuthorizationContext(
                                principal.identityId(),
                                tenantId,
                                membership.id()
                        )))
                .orElseGet(() -> Mono.error(new TenantContextUnavailableException(
                        TenantContextUnavailableException.Reason.TENANT_CLAIM_ABSENT,
                        TENANT_CLAIM_ABSENT_MESSAGE
                )));
    }
}
