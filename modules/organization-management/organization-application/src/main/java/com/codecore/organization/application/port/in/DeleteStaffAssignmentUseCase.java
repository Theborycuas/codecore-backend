package com.codecore.organization.application.port.in;

import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import reactor.core.publisher.Mono;

public interface DeleteStaffAssignmentUseCase {

    Mono<Void> delete(StaffAssignmentId assignmentId);
}
