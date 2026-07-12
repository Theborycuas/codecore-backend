package com.codecore.inventory.infrastructure.adapters;

import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.inventory.application.port.out.TenantContextAccessor;
import com.codecore.inventory.domain.valueobject.TenantId;
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
