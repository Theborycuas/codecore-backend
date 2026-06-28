package com.codecore.organization.interfaces.http.admin.dto;

import com.codecore.organization.application.dto.AdminOfficeView;
import com.codecore.organization.application.dto.PagedResult;

import java.util.List;

public record PagedOfficeResponse(
        List<OfficeResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedOfficeResponse from(PagedResult<AdminOfficeView> result) {
        return new PagedOfficeResponse(
                result.content().stream().map(OfficeResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
