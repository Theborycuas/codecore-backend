package com.codecore.organization.application.port.in;

import com.codecore.organization.application.command.CreateStaffAssignmentCommand;
import com.codecore.organization.application.dto.AdminStaffAssignmentView;
import reactor.core.publisher.Mono;

public interface CreateStaffAssignmentUseCase {

    Mono<AdminStaffAssignmentView> execute(CreateStaffAssignmentCommand command);
}
