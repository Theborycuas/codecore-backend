package com.codecore.access.interfaces.http.admin.dto;

import com.codecore.access.application.dto.AdminInvitationView;
import com.codecore.access.application.dto.PagedResult;

import java.util.List;

public record PagedInvitationResponse(
        List<InvitationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedInvitationResponse from(PagedResult<AdminInvitationView> result) {
        return new PagedInvitationResponse(
                result.content().stream().map(InvitationResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
