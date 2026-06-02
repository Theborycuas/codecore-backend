package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.RegisterIdentityCommand;
import com.codecore.iam.application.dto.RegisterIdentityResult;
import reactor.core.publisher.Mono;

public interface RegisterIdentityUseCase {

    Mono<RegisterIdentityResult> execute(RegisterIdentityCommand command);
}
