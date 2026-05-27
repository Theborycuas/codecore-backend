package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.LogoutCommand;
import reactor.core.publisher.Mono;

public interface LogoutUseCase {

    Mono<Void> execute(LogoutCommand command);
}
