package com.codecore.access.application.port.in;

import com.codecore.access.application.dto.AdminInvitationView;
import com.codecore.access.domain.valueobject.InvitationId;
import reactor.core.publisher.Mono;

public interface GetInvitationUseCase {

    Mono<AdminInvitationView> execute(InvitationId invitationId);
}
