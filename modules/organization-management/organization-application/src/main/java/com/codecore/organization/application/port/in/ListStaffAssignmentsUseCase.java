package com.codecore.organization.application.port.in;

import com.codecore.organization.application.command.CreateStaffAssignmentCommand;
import com.codecore.organization.application.dto.AdminStaffAssignmentView;
import com.codecore.organization.application.dto.PagedResult;
import com.codecore.organization.application.query.PageQuery;
import com.codecore.organization.application.query.StaffAssignmentListFilter;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import reactor.core.publisher.Mono;

public interface ListStaffAssignmentsUseCase {

    Mono<PagedResult<AdminStaffAssignmentView>> execute(StaffAssignmentListFilter filter, PageQuery pageQuery);
}
