package com.codecore.iam.application.port.in;

import com.codecore.iam.application.dto.AdminRolePermissionView;
import com.codecore.iam.domain.valueobject.RoleId;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GetAdminRolePermissionsUseCase {

    Mono<List<AdminRolePermissionView>> execute(RoleId roleId);
}
