package com.codecore.iam.interfaces.http.security;

import com.codecore.iam.application.dto.AccessTokenClaims;
import com.codecore.iam.application.dto.AuthenticatedPrincipal;
import com.codecore.iam.application.port.out.TokenValidator;
import com.codecore.iam.domain.exception.ExpiredTokenException;
import com.codecore.iam.domain.exception.InvalidTokenException;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.infrastructure.security.JwtTokenProvider;
import com.codecore.iam.infrastructure.security.JwtTokenValidator;
import com.codecore.iam.infrastructure.security.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationWebFilterTest {

    private static final String SECRET = "codecore-test-jwt-secret-key-minimum-32-characters-long!!";
    private static final String ISSUER = "codecore-test";

    private JwtTokenProvider tokenProvider;
    private JwtAuthenticationWebFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer(ISSUER);
        properties.setExpiration(Duration.ofSeconds(900));
        tokenProvider = new JwtTokenProvider(properties);
        filter = new JwtAuthenticationWebFilter(new JwtTokenValidator(properties));
    }

    @Test
    void shouldPassThroughPublicLoginPathWithoutCallingValidator() {
        TokenValidator validator = mock(TokenValidator.class);
        JwtAuthenticationWebFilter publicFilter = new JwtAuthenticationWebFilter(validator);

        MockServerWebExchange exchange = exchange(
                HttpMethod.POST,
                "/api/v1/auth/login",
                null
        );

        StepVerifier.create(publicFilter.filter(exchange, passThroughChain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        verify(validator, never()).validate(anyString());
    }

    @Test
    void shouldReturn401WhenProtectedPathWithoutAuthorizationHeader() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/v1/auth/me", null);

        StepVerifier.create(filter.filter(exchange, passThroughChain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenAuthorizationSchemeIsNotBearer() {
        MockServerWebExchange exchange = exchange(
                HttpMethod.GET,
                "/api/v1/auth/me",
                "Basic abc"
        );

        StepVerifier.create(filter.filter(exchange, passThroughChain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenTokenInvalid() {
        TokenValidator validator = mock(TokenValidator.class);
        when(validator.validate("bad-token")).thenThrow(new InvalidTokenException("Invalid token"));
        JwtAuthenticationWebFilter invalidFilter = new JwtAuthenticationWebFilter(validator);

        MockServerWebExchange exchange = exchange(
                HttpMethod.GET,
                "/api/v1/auth/me",
                "Bearer bad-token"
        );

        StepVerifier.create(invalidFilter.filter(exchange, passThroughChain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WhenTokenExpired() {
        TokenValidator validator = mock(TokenValidator.class);
        when(validator.validate("expired")).thenThrow(new ExpiredTokenException("Token has expired"));
        JwtAuthenticationWebFilter expiredFilter = new JwtAuthenticationWebFilter(validator);

        MockServerWebExchange exchange = exchange(
                HttpMethod.GET,
                "/api/v1/auth/me",
                "Bearer expired"
        );

        StepVerifier.create(expiredFilter.filter(exchange, passThroughChain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldPropagatePrincipalInReactorContextForValidToken() {
        String token = tokenProvider.generateAccessToken(
                new AccessTokenClaims(
                        "44444444-4444-4444-4444-444444444444",
                        "me.filter@codecore.local",
                        "ACTIVE"
                )
        ).accessToken();

        MockServerWebExchange exchange = exchange(
                HttpMethod.GET,
                "/api/v1/auth/me",
                "Bearer " + token
        );

        AtomicReference<Optional<AuthenticatedPrincipal>> captured = new AtomicReference<>();

        WebFilterChain chain = serverExchange -> Mono.deferContextual(ctx -> {
            captured.set(AuthenticationContext.get(ctx));
            return serverExchange.getResponse().setComplete();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(captured.get()).isPresent();
        assertThat(captured.get().orElseThrow().email()).isEqualTo("me.filter@codecore.local");
        assertThat(captured.get().orElseThrow().identityId())
                .isEqualTo(new IdentityId("44444444-4444-4444-4444-444444444444"));
        assertThat(captured.get().orElseThrow().status()).isEqualTo(IdentityStatus.ACTIVE);
    }

    private static WebFilterChain passThroughChain() {
        return exchange -> exchange.getResponse().setComplete();
    }

    private static MockServerWebExchange exchange(HttpMethod method, String path, String authorization) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.method(method, path);
        if (authorization != null) {
            builder.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return MockServerWebExchange.from(builder.build());
    }
}
