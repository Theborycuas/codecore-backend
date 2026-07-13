package com.codecore.access.interfaces.http.publicapi;

import com.codecore.access.application.command.AcceptInvitationCommand;
import com.codecore.access.application.port.in.AcceptInvitationUseCase;
import com.codecore.access.interfaces.http.publicapi.dto.AcceptInvitationRequest;
import com.codecore.access.interfaces.http.publicapi.dto.AcceptInvitationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Public accept endpoint — no {@code @RequiresPermission}; must be {@code permitAll}.
 */
@RestController
@Tag(name = "Access Invitation Accept", description = "Public invitation accept (token-based)")
public class InvitationAcceptController {

    private final AcceptInvitationUseCase acceptInvitationUseCase;

    public InvitationAcceptController(AcceptInvitationUseCase acceptInvitationUseCase) {
        this.acceptInvitationUseCase = acceptInvitationUseCase;
    }

    @PostMapping(InvitationAcceptApiPaths.ACCEPT)
    public Mono<AcceptInvitationResponse> accept(@Valid @RequestBody AcceptInvitationRequest request) {
        return acceptInvitationUseCase
                .execute(new AcceptInvitationCommand(request.token(), request.password()))
                .map(AcceptInvitationResponse::from);
    }
}
