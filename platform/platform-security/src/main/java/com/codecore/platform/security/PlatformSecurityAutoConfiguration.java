package com.codecore.platform.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableWebFluxSecurity
public class PlatformSecurityAutoConfiguration {

    @Bean
    SecurityWebFilterChain platformSecurityWebFilterChain(
            ServerHttpSecurity http,
            ObjectProvider<ReactiveAuthorizationManager<AuthorizationContext>> authorizationManagerProvider
    ) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // Default security headers conflict with WebTestClient (ReadOnlyHttpHeaders after commit).
                // Production HSTS/CSP belong at the reverse proxy (ADR-009).
                .headers(ServerHttpSecurity.HeaderSpec::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, authException) -> {
                            if (exchange.getResponse().isCommitted()) {
                                return Mono.empty();
                            }
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, denied) -> {
                            if (exchange.getResponse().isCommitted()) {
                                return Mono.empty();
                            }
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        }))
                .authorizeExchange(exchanges -> {
                    exchanges
                            .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                            .pathMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                            .pathMatchers(HttpMethod.GET, "/actuator/health").permitAll();
                    ReactiveAuthorizationManager<AuthorizationContext> manager =
                            authorizationManagerProvider.getIfAvailable();
                    if (manager != null) {
                        exchanges.anyExchange().access(manager);
                    } else {
                        exchanges.anyExchange().authenticated();
                    }
                });
        return http.build();
    }
}
