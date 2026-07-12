package com.codecore.encounter.infrastructure.adapters;

import com.codecore.encounter.application.port.out.TenantContextAccessor;
import com.codecore.encounter.domain.valueobject.TenantId;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
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
