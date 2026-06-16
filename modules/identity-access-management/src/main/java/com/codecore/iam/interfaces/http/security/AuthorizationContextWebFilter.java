package com.codecore.iam.interfaces.http.security;

import com.codecore.iam.application.ReactorAuthorizationContextAccessor;
import com.codecore.iam.domain.exception.IdentityNotMemberOfTenantException;
import com.codecore.iam.domain.exception.TenantContextUnavailableException;
import org.springframework.core.annotation.Order;
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

    public AuthorizationContextWebFilter(ReactorAuthorizationContextAccessor authorizationContextAccessor) {
        this.authorizationContextAccessor = authorizationContextAccessor;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (PublicApiPaths.isPublic(exchange)) {
            return chain.filter(exchange);
        }

        return AuthenticationContext.currentPrincipal()
                .flatMap(authorizationContextAccessor::resolveForPrincipal)
                .flatMap(ctx -> chain.filter(exchange)
                        .contextWrite(reactorCtx -> AuthorizationReactorContext.write(reactorCtx, ctx)))
                .onErrorResume(IdentityNotMemberOfTenantException.class, ex -> forbidden(exchange))
                .onErrorResume(TenantContextUnavailableException.class, ex -> forbidden(exchange))
                .switchIfEmpty(chain.filter(exchange));
    }

    private static Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }
}
