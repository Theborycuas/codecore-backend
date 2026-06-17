package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.UpdateAdminRoleCommand;
import com.codecore.iam.application.dto.AdminRoleView;
import reactor.core.publisher.Mono;

public interface UpdateAdminRoleUseCase {

    Mono<AdminRoleView> execute(UpdateAdminRoleCommand command);
}
