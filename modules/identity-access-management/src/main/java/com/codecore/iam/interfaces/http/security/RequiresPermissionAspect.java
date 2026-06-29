package com.codecore.iam.interfaces.http.security;

import com.codecore.iam.application.port.in.AuthorizationService;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.domain.exception.AuthorizationDeniedException;
import com.codecore.iam.domain.valueobject.PermissionCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Enforces {@link RequiresPermission} on reactive controller methods.
 */
@Aspect
@Component
public class RequiresPermissionAspect {

    private static final String CONTEXT_UNAVAILABLE =
            "Authorization context is not available for permission check";

    private final AuthorizationContextAccessor authorizationContextAccessor;
    private final AuthorizationService authorizationService;

    public RequiresPermissionAspect(
            AuthorizationContextAccessor authorizationContextAccessor,
            AuthorizationService authorizationService
    ) {
        this.authorizationContextAccessor = authorizationContextAccessor;
        this.authorizationService = authorizationService;
    }

    @Around("@annotation(requiresPermission)")
    public Object enforceRequiresPermission(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) {
        PermissionCode permissionCode = PermissionCode.of(requiresPermission.value());
        final Mono<?> endpoint;
        try {
            endpoint = (Mono<?>) joinPoint.proceed();
        } catch (Throwable ex) {
            return Mono.error(ex);
        }
        return authorizationContextAccessor.current()
                .switchIfEmpty(Mono.error(new AuthorizationDeniedException(CONTEXT_UNAVAILABLE)))
                .flatMap(ctx -> authorizationService.hasPermission(ctx, permissionCode))
                .flatMap(granted -> {
                    if (!granted) {
                        return Mono.error(new AuthorizationDeniedException(
                                "Required permission denied: " + permissionCode.value()
                        ));
                    }
                    return endpoint;
                });
    }
}
