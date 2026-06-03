package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.AuthenticationCommand;
import com.codecore.iam.application.dto.AuthenticationResponse;
import reactor.core.publisher.Mono;

public interface AuthenticateIdentityUseCase {

    Mono<AuthenticationResponse> execute(AuthenticationCommand command);
}
