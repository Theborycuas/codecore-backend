package com.codecore.iam.interfaces.http.security;

import com.codecore.iam.application.ReactorAuthorizationContextAccessor;
import com.codecore.iam.application.TenantOperationalGuard;
import com.codecore.iam.application.dto.AuthenticatedPrincipal;
import com.codecore.iam.domain.exception.IdentityNotMemberOfTenantException;
import com.codecore.iam.domain.exception.TenantContextUnavailableException;
import com.codecore.iam.domain.exception.TenantNotOperationalException;
import com.codecore.iam.interfaces.http.admin.IamAdminApiPaths;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Resolves active membership authorization context after JWT authentication (ADR-007).
 */
@Component
@Order(-98)
public class AuthorizationContextWebFilter implements WebFilter {

    private final ReactorAuthorizationContextAccessor authorizationContextAccessor;
    private final TenantOperationalGuard tenantOperationalGuard;

    public AuthorizationContextWebFilter(
            ReactorAuthorizationContextAccessor authorizationContextAccessor,
            TenantOperationalGuard tenantOperationalGuard
    ) {
        this.authorizationContextAccessor = authorizationContextAccessor;
        this.tenantOperationalGuard = tenantOperationalGuard;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (PublicApiPaths.isPublic(exchange)) {
            return chain.filter(exchange);
        }

        return AuthenticationContext.currentPrincipal()
                .flatMap(principal -> assertTenantOperationalIfRequired(exchange, principal)
                        .then(authorizationContextAccessor.resolveForPrincipal(principal)))
                .flatMap(ctx -> Mono.defer(() -> chain.filter(exchange))
                        .contextWrite(reactorCtx -> AuthorizationReactorContext.write(reactorCtx, ctx)))
                .onErrorResume(IdentityNotMemberOfTenantException.class, ex -> forbidden(exchange))
                .onErrorResume(TenantContextUnavailableException.class, ex -> forbidden(exchange))
                .onErrorResume(TenantNotOperationalException.class, ex -> forbidden(exchange));
    }

    private Mono<Void> assertTenantOperationalIfRequired(
            ServerWebExchange exchange,
            AuthenticatedPrincipal principal
    ) {
        if (isTenantReactivationRequest(exchange)) {
            return Mono.empty();
        }
        return principal.tenantId()
                .map(tenantOperationalGuard::assertOperational)
                .orElse(Mono.empty());
    }

    static boolean isTenantReactivationRequest(ServerWebExchange exchange) {
        if (exchange.getRequest().getMethod() != HttpMethod.PUT) {
            return false;
        }
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        return (IamAdminApiPaths.TENANTS + "/current").equals(path);
    }

    private static Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }
}
