package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.application.dto.PagedResult;

import java.util.List;

public record PagedRoleResponse(
        List<RoleResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedRoleResponse from(PagedResult<com.codecore.iam.application.dto.AdminRoleView> result) {
        return new PagedRoleResponse(
                result.content().stream().map(RoleResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
