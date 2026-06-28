package com.codecore.organization.application.port.in;

import com.codecore.organization.application.command.UpdateOrganizationCommand;
import com.codecore.organization.application.dto.AdminOrganizationView;
import reactor.core.publisher.Mono;

public interface UpdateOrganizationUseCase {

    Mono<AdminOrganizationView> execute(UpdateOrganizationCommand command);
}
