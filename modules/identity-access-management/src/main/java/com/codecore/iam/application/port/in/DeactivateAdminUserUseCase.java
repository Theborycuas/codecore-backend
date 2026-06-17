package com.codecore.iam.application.port.in;

import com.codecore.iam.domain.valueobject.IdentityId;
import reactor.core.publisher.Mono;

public interface DeactivateAdminUserUseCase {

    Mono<Void> deactivate(IdentityId identityId);
}
