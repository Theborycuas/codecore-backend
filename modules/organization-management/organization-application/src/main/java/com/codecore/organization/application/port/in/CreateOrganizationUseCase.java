package com.codecore.organization.application.port.in;

import com.codecore.organization.application.command.CreateOrganizationCommand;
import com.codecore.organization.application.dto.AdminOrganizationView;
import reactor.core.publisher.Mono;

public interface CreateOrganizationUseCase {

    Mono<AdminOrganizationView> execute(CreateOrganizationCommand command);
}
