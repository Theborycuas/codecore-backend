package com.codecore.iam.application.port.in;

import com.codecore.iam.application.dto.AdminRoleView;
import com.codecore.iam.domain.valueobject.RoleId;
import reactor.core.publisher.Mono;

public interface GetAdminRoleUseCase {

    Mono<AdminRoleView> execute(RoleId roleId);
}
