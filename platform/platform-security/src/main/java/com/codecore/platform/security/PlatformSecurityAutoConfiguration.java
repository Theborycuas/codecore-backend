package com.codecore.platform.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.server.WebFilter;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableWebFluxSecurity
public class PlatformSecurityAutoConfiguration {

    @Bean
    SecurityWebFilterChain platformSecurityWebFilterChain(
            ServerHttpSecurity http,
            @Autowired(required = false)
            @Qualifier("jwtAuthenticationWebFilter") WebFilter jwtAuthenticationWebFilter,
            ObjectProvider<ReactiveAuthorizationManager<AuthorizationContext>> authorizationManagerProvider
    ) {
        if (jwtAuthenticationWebFilter != null) {
            http.addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        }

        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> {
                    exchanges
                            .pathMatchers(HttpMethod.POST, "/api/v1/identities").permitAll()
                            .pathMatchers(HttpMethod.POST, "/api/v1/tenants").permitAll()
                            .pathMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                            .pathMatchers(HttpMethod.GET, "/actuator/health").permitAll();
                    if (jwtAuthenticationWebFilter != null) {
                        ReactiveAuthorizationManager<AuthorizationContext> manager =
                                authorizationManagerProvider.getIfAvailable();
                        if (manager != null) {
                            exchanges.anyExchange().access(manager);
                        } else {
                            exchanges.anyExchange().denyAll();
                        }
                    } else {
                        exchanges.anyExchange().authenticated();
                    }
                });

        return http.build();
    }
}
