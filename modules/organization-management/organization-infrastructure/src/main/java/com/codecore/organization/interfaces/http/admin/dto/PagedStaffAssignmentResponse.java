package com.codecore.organization.interfaces.http.admin.dto;

import com.codecore.organization.application.dto.AdminStaffAssignmentView;
import com.codecore.organization.application.dto.PagedResult;

import java.util.List;

public record PagedStaffAssignmentResponse(
        List<StaffAssignmentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedStaffAssignmentResponse from(PagedResult<AdminStaffAssignmentView> result) {
        return new PagedStaffAssignmentResponse(
                result.content().stream().map(StaffAssignmentResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
