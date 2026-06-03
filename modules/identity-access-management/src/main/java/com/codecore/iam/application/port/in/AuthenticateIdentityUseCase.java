package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.AuthenticationCommand;
import com.codecore.iam.application.dto.AuthenticationResult;
import reactor.core.publisher.Mono;

public interface AuthenticateIdentityUseCase {

    Mono<AuthenticationResult> execute(AuthenticationCommand command);
}
