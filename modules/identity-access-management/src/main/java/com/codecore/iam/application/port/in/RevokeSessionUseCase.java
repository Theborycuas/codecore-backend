package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.RevokeSessionCommand;
import reactor.core.publisher.Mono;

public interface RevokeSessionUseCase {

    Mono<Void> execute(RevokeSessionCommand command);
}
