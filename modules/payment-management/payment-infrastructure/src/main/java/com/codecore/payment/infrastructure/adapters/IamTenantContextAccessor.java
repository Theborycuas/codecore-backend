package com.codecore.payment.infrastructure.adapters;

import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.payment.application.port.out.TenantContextAccessor;
import com.codecore.payment.domain.valueobject.TenantId;
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
