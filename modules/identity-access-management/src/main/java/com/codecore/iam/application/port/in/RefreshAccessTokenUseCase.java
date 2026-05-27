package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.RefreshAccessTokenCommand;
import com.codecore.iam.application.dto.AuthenticationResultDto;
import reactor.core.publisher.Mono;

public interface RefreshAccessTokenUseCase {

    Mono<AuthenticationResultDto> execute(RefreshAccessTokenCommand command);
}
