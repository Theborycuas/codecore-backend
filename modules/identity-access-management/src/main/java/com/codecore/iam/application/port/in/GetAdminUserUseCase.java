package com.codecore.iam.application.port.in;

import com.codecore.iam.application.dto.AdminUserView;
import com.codecore.iam.domain.valueobject.IdentityId;
import reactor.core.publisher.Mono;

public interface GetAdminUserUseCase {

    Mono<AdminUserView> execute(IdentityId identityId);
}
