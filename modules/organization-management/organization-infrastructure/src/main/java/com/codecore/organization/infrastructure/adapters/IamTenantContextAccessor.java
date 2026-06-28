package com.codecore.organization.infrastructure.adapters;

import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.organization.application.port.out.TenantContextAccessor;
import com.codecore.organization.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

public final class IamTenantContextAccessor implements TenantContextAccessor {

    private final AuthorizationContextAccessor authorizationContextAccessor;

    public IamTenantContextAccessor(AuthorizationContextAccessor authorizationContextAccessor) {
        this.authorizationContextAccessor = authorizationContextAccessor;
    }

    @Override
    public Mono<TenantId> currentTenantId() {
        return authorizationContextAccessor.current()
                .map(ctx -> new TenantId(ctx.tenantId().value()));
    }
}
