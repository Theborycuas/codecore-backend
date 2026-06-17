package com.codecore.iam.application.port.in;

import com.codecore.iam.domain.valueobject.MembershipId;
import reactor.core.publisher.Mono;

public interface DeactivateAdminMembershipUseCase {

    Mono<Void> deactivate(MembershipId membershipId);
}
