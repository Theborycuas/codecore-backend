package com.codecore.iam.interfaces.http.security;

import com.codecore.iam.application.dto.AuthenticatedPrincipal;
import com.codecore.iam.application.port.out.TenantContext;
import com.codecore.iam.domain.exception.TenantContextUnavailableException;
import com.codecore.iam.domain.valueobject.TenantId;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Resolves the current tenant from {@link AuthenticationContext} (Reactor Context populated by JWT filter).
 */
@Component
public class ReactorTenantContext implements TenantContext {

    private static final String NOT_AUTHENTICATED_MESSAGE =
            "Authenticated principal is not available for the current request";
    private static final String TENANT_CLAIM_ABSENT_MESSAGE =
            "Tenant claim is not present in the authenticated JWT";

    @Override
    public Mono<TenantId> currentTenant() {
        return AuthenticationContext.currentPrincipal()
                .switchIfEmpty(Mono.error(new TenantContextUnavailableException(
                        TenantContextUnavailableException.Reason.NOT_AUTHENTICATED,
                        NOT_AUTHENTICATED_MESSAGE
                )))
                .flatMap(ReactorTenantContext::requireTenantId);
    }

    private static Mono<TenantId> requireTenantId(AuthenticatedPrincipal principal) {
        return principal.tenantId()
                .map(Mono::just)
                .orElseGet(() -> Mono.error(new TenantContextUnavailableException(
                        TenantContextUnavailableException.Reason.TENANT_CLAIM_ABSENT,
                        TENANT_CLAIM_ABSENT_MESSAGE
                )));
    }
}
