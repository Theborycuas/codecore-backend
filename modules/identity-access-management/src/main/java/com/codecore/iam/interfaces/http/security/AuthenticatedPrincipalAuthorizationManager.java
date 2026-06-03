package com.codecore.iam.interfaces.http.security;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Allows access when {@link AuthenticationContext} holds a validated principal (no roles/permissions).
 */
@Component
public class AuthenticatedPrincipalAuthorizationManager
        implements ReactiveAuthorizationManager<AuthorizationContext> {

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
        return Mono.deferContextual(ctx -> Mono.just(
                new AuthorizationDecision(AuthenticationContext.get(ctx).isPresent())
        ));
    }
}
