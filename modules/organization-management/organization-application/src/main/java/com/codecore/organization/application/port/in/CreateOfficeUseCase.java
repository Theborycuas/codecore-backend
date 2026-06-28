package com.codecore.organization.application.port.in;

import com.codecore.organization.application.command.CreateOfficeCommand;
import com.codecore.organization.application.dto.AdminOfficeView;
import reactor.core.publisher.Mono;

public interface CreateOfficeUseCase {

    Mono<AdminOfficeView> execute(CreateOfficeCommand command);
}
