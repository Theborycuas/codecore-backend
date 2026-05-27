package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.CompletePasswordResetCommand;
import reactor.core.publisher.Mono;

public interface CompletePasswordResetUseCase {

    Mono<Void> execute(CompletePasswordResetCommand command);
}
