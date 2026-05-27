package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.AuthenticateCommand;
import com.codecore.iam.application.dto.AuthenticationResultDto;
import reactor.core.publisher.Mono;

public interface AuthenticateUseCase {

    Mono<AuthenticationResultDto> execute(AuthenticateCommand command);
}
