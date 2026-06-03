package com.codecore.iam.interfaces.http.security;

import com.codecore.iam.application.dto.AuthenticatedPrincipal;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Optional;

/**
 * Request-scoped access to {@link AuthenticatedPrincipal} via Reactor Context (no Spring Security UserDetails).
 */
public final class AuthenticationContext {

    static final String CONTEXT_KEY = "codecore.iam.authenticated-principal";

    private AuthenticationContext() {
    }

    public static Context write(Context context, AuthenticatedPrincipal principal) {
        return context.put(CONTEXT_KEY, principal);
    }

    public static Optional<AuthenticatedPrincipal> get(ContextView context) {
        return context.getOrEmpty(CONTEXT_KEY);
    }

    public static Mono<AuthenticatedPrincipal> currentPrincipal() {
        return Mono.deferContextual(ctx -> {
            Optional<AuthenticatedPrincipal> principal = get(ctx);
            return principal.map(Mono::just).orElseGet(Mono::empty);
        });
    }
}
