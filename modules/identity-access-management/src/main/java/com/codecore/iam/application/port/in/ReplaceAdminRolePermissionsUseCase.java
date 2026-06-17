package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.ReplaceAdminRolePermissionsCommand;
import com.codecore.iam.application.dto.AdminRolePermissionView;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ReplaceAdminRolePermissionsUseCase {

    Mono<List<AdminRolePermissionView>> execute(ReplaceAdminRolePermissionsCommand command);
}
