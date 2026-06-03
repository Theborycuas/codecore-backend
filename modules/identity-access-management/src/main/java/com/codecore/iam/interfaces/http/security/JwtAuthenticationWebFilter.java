package com.codecore.iam.interfaces.http.security;

import com.codecore.iam.application.dto.AuthenticatedPrincipal;
import com.codecore.iam.application.port.out.TokenValidator;
import com.codecore.iam.domain.exception.ExpiredTokenException;
import com.codecore.iam.domain.exception.InvalidTokenException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Validates Bearer JWT on non-public routes and propagates {@link AuthenticatedPrincipal} in Reactor Context.
 */
@Component("jwtAuthenticationWebFilter")
@Order(-100)
public class JwtAuthenticationWebFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenValidator tokenValidator;

    public JwtAuthenticationWebFilter(TokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (PublicApiPaths.isPublic(exchange)) {
            return chain.filter(exchange);
        }

        String bearerToken = extractBearerToken(exchange);
        if (bearerToken == null) {
            return unauthorized(exchange);
        }

        try {
            AuthenticatedPrincipal principal = tokenValidator.validate(bearerToken);
            return chain.filter(exchange)
                    .contextWrite(ctx -> AuthenticationContext.write(ctx, principal));
        } catch (InvalidTokenException | ExpiredTokenException ex) {
            return unauthorized(exchange);
        }
    }

    private static String extractBearerToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            return null;
        }
        String trimmed = header.trim();
        if (!trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = trimmed.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
