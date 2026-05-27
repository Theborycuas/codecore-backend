package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.ChangePasswordCommand;
import reactor.core.publisher.Mono;

public interface ChangePasswordUseCase {

    Mono<Void> execute(ChangePasswordCommand command);
}
