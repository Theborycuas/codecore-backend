package com.codecore.iam.interfaces.http.security;

import com.codecore.iam.application.dto.AuthorizationContext;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Optional;

/**
 * Request-scoped access to {@link AuthorizationContext} via Reactor Context.
 */
public final class AuthorizationReactorContext {

    static final String CONTEXT_KEY = "codecore.iam.authorization-context";

    private AuthorizationReactorContext() {
    }

    public static Context write(Context context, AuthorizationContext authorizationContext) {
        return context.put(CONTEXT_KEY, authorizationContext);
    }

    public static Optional<AuthorizationContext> get(ContextView context) {
        return context.getOrEmpty(CONTEXT_KEY);
    }

    public static Mono<AuthorizationContext> current() {
        return Mono.deferContextual(ctx -> {
            Optional<AuthorizationContext> authorizationContext = get(ctx);
            return authorizationContext.map(Mono::just).orElseGet(Mono::empty);
        });
    }
}
