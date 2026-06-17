package com.codecore.iam.application.port.in;

import com.codecore.iam.application.dto.AdminPermissionView;
import com.codecore.iam.domain.valueobject.PermissionId;
import reactor.core.publisher.Mono;

public interface GetAdminPermissionUseCase {

    Mono<AdminPermissionView> execute(PermissionId permissionId);
}
