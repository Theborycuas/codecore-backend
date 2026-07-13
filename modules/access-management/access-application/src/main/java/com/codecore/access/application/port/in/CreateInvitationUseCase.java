package com.codecore.access.application.port.in;

import com.codecore.access.application.command.CreateInvitationCommand;
import com.codecore.access.application.dto.CreateInvitationResult;
import reactor.core.publisher.Mono;

public interface CreateInvitationUseCase {

    Mono<CreateInvitationResult> execute(CreateInvitationCommand command);
}
