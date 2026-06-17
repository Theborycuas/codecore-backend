package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.UpdateAdminMembershipCommand;
import com.codecore.iam.application.dto.AdminMembershipView;
import reactor.core.publisher.Mono;

public interface UpdateAdminMembershipUseCase {

    Mono<AdminMembershipView> execute(UpdateAdminMembershipCommand command);
}
