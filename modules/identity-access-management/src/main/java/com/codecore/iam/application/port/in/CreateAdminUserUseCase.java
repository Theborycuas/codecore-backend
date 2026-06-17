package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.CreateAdminUserCommand;
import com.codecore.iam.application.dto.AdminUserView;
import com.codecore.iam.application.query.PageQuery;
import com.codecore.iam.application.dto.PagedResult;
import reactor.core.publisher.Mono;

public interface CreateAdminUserUseCase {

    Mono<AdminUserView> execute(CreateAdminUserCommand command);
}
