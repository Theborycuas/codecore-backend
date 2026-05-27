package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.RequestPasswordResetCommand;
import reactor.core.publisher.Mono;

public interface RequestPasswordResetUseCase {

    Mono<Void> execute(RequestPasswordResetCommand command);
}
