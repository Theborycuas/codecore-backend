package com.codecore.access.infrastructure.adapters;

import com.codecore.access.application.port.out.MembershipContextAccessor;
import com.codecore.access.domain.valueobject.MembershipId;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import reactor.core.publisher.Mono;

public final class IamMembershipContextAccessor implements MembershipContextAccessor {

    private final AuthorizationContextAccessor authorizationContextAccessor;

    public IamMembershipContextAccessor(AuthorizationContextAccessor authorizationContextAccessor) {
        this.authorizationContextAccessor = authorizationContextAccessor;
    }

    @Override
    public Mono<MembershipId> currentMembershipId() {
        return authorizationContextAccessor.current()
                .map(ctx -> MembershipId.of(ctx.membershipId().value()));
    }
}
