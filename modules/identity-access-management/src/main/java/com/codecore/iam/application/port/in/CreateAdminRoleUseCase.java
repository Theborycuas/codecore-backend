package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.CreateAdminRoleCommand;
import com.codecore.iam.application.dto.AdminRoleView;
import reactor.core.publisher.Mono;

public interface CreateAdminRoleUseCase {

    Mono<AdminRoleView> execute(CreateAdminRoleCommand command);
}
