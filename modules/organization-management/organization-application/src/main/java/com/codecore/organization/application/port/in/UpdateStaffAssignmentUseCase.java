package com.codecore.organization.application.port.in;

import com.codecore.organization.application.command.UpdateStaffAssignmentCommand;
import com.codecore.organization.application.dto.AdminStaffAssignmentView;
import reactor.core.publisher.Mono;

public interface UpdateStaffAssignmentUseCase {

    Mono<AdminStaffAssignmentView> execute(UpdateStaffAssignmentCommand command);
}
