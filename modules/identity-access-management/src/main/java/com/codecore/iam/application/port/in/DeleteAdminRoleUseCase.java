package com.codecore.iam.application.port.in;

import com.codecore.iam.domain.valueobject.RoleId;
import reactor.core.publisher.Mono;

public interface DeleteAdminRoleUseCase {

    Mono<Void> delete(RoleId roleId);
}
