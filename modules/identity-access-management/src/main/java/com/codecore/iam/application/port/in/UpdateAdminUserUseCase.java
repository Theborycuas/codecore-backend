package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.UpdateAdminUserCommand;
import com.codecore.iam.application.dto.AdminUserView;
import reactor.core.publisher.Mono;

public interface UpdateAdminUserUseCase {

    Mono<AdminUserView> execute(UpdateAdminUserCommand command);
}
