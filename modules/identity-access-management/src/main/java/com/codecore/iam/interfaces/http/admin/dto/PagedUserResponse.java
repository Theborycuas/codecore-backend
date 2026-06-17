package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.application.dto.PagedResult;

import java.util.List;

public record PagedUserResponse(
        List<UserResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedUserResponse from(PagedResult<com.codecore.iam.application.dto.AdminUserView> result) {
        return new PagedUserResponse(
                result.content().stream().map(UserResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
