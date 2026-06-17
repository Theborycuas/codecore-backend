package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.CreateAdminMembershipCommand;
import com.codecore.iam.application.dto.AdminMembershipView;
import reactor.core.publisher.Mono;

public interface CreateAdminMembershipUseCase {

    Mono<AdminMembershipView> execute(CreateAdminMembershipCommand command);
}
