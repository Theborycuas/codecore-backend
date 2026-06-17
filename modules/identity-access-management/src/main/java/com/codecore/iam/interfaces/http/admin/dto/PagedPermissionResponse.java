package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.application.dto.PagedResult;

import java.util.List;

public record PagedPermissionResponse(
        List<PermissionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedPermissionResponse from(PagedResult<com.codecore.iam.application.dto.AdminPermissionView> result) {
        return new PagedPermissionResponse(
                result.content().stream().map(PermissionResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
