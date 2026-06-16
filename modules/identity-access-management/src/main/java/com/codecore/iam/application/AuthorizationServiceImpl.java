package com.codecore.iam.application;

import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.port.in.AuthorizationService;
import com.codecore.iam.application.port.out.AuthorizationQueryRepository;
import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.RoleCode;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates membership-scoped RBAC grants via {@link AuthorizationQueryRepository}.
 */
public class AuthorizationServiceImpl implements AuthorizationService {

    private final AuthorizationQueryRepository authorizationQueryRepository;

    public AuthorizationServiceImpl(AuthorizationQueryRepository authorizationQueryRepository) {
        this.authorizationQueryRepository = Objects.requireNonNull(
                authorizationQueryRepository,
                "authorizationQueryRepository"
        );
    }

    @Override
    public Mono<Boolean> hasPermission(AuthorizationContext context, PermissionCode permissionCode) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(permissionCode, "permissionCode");
        return authorizationQueryRepository.existsPermissionForMembership(
                context.membershipId(),
                permissionCode
        );
    }

    @Override
    public Mono<Boolean> hasAnyPermission(AuthorizationContext context, PermissionCode... permissionCodes) {
        Objects.requireNonNull(context, "context");
        if (permissionCodes == null || permissionCodes.length == 0) {
            return Mono.just(false);
        }
        List<String> codes = Arrays.stream(permissionCodes)
                .filter(Objects::nonNull)
                .map(PermissionCode::value)
                .toList();
        if (codes.isEmpty()) {
            return Mono.just(false);
        }
        return authorizationQueryRepository.existsAnyPermissionForMembership(context.membershipId(), codes);
    }

    @Override
    public Mono<Boolean> hasRole(AuthorizationContext context, RoleCode roleCode) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(roleCode, "roleCode");
        return authorizationQueryRepository.existsRoleForMembership(context.membershipId(), roleCode);
    }
}
