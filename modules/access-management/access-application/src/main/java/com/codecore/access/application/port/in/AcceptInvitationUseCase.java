package com.codecore.access.application.port.in;

import com.codecore.access.application.command.AcceptInvitationCommand;
import com.codecore.access.application.dto.AcceptInvitationResult;
import reactor.core.publisher.Mono;

public interface AcceptInvitationUseCase {

    Mono<AcceptInvitationResult> execute(AcceptInvitationCommand command);
}
