package com.codecore.iam.interfaces.http.security;

import org.springframework.http.HttpMethod;
import org.springframework.web.server.ServerWebExchange;

/**
 * Public routes that skip JWT validation (must stay aligned with {@code PlatformSecurityAutoConfiguration}).
 */
public final class PublicApiPaths {

    private PublicApiPaths() {
    }

    public static boolean isPublic(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getPath().pathWithinApplication().value();

        if (method == HttpMethod.GET && "/actuator/health".equals(path)) {
            return true;
        }
        if (method == HttpMethod.POST && "/api/v1/auth/login".equals(path)) {
            return true;
        }
        if (method == HttpMethod.POST && "/api/v1/identities".equals(path)) {
            return true;
        }
        if (method == HttpMethod.POST && "/api/v1/tenants".equals(path)) {
            return true;
        }
        return false;
    }
}
