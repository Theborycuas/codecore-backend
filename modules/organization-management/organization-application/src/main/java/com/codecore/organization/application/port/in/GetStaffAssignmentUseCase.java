package com.codecore.organization.application.port.in;

import com.codecore.organization.application.dto.AdminStaffAssignmentView;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import reactor.core.publisher.Mono;

public interface GetStaffAssignmentUseCase {

    Mono<AdminStaffAssignmentView> execute(StaffAssignmentId assignmentId);
}
