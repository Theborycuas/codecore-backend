package com.codecore.organization.application.port.in;

import com.codecore.organization.application.command.UpdateOfficeCommand;
import com.codecore.organization.application.dto.AdminOfficeView;
import reactor.core.publisher.Mono;

public interface UpdateOfficeUseCase {

    Mono<AdminOfficeView> execute(UpdateOfficeCommand command);
}
