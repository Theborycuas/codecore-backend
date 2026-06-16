package com.codecore.iam.application.port.in;

import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.RoleCode;
import reactor.core.publisher.Mono;

/**
 * Runtime authorization evaluation over Membership → Role → Permission (ADR-007).
 */
public interface AuthorizationService {

    Mono<Boolean> hasPermission(AuthorizationContext context, PermissionCode permissionCode);

    Mono<Boolean> hasAnyPermission(AuthorizationContext context, PermissionCode... permissionCodes);

    Mono<Boolean> hasRole(AuthorizationContext context, RoleCode roleCode);
}
