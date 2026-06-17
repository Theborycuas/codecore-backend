package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.application.dto.PagedResult;

import java.util.List;

public record PagedMembershipResponse(
        List<MembershipResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedMembershipResponse from(PagedResult<com.codecore.iam.application.dto.AdminMembershipView> result) {
        return new PagedMembershipResponse(
                result.content().stream().map(MembershipResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
