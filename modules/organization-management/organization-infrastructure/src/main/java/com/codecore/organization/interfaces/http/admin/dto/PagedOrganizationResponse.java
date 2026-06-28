package com.codecore.organization.interfaces.http.admin.dto;

import com.codecore.organization.application.dto.AdminOrganizationView;
import com.codecore.organization.application.dto.PagedResult;

import java.util.List;

public record PagedOrganizationResponse(
        List<OrganizationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedOrganizationResponse from(PagedResult<AdminOrganizationView> result) {
        return new PagedOrganizationResponse(
                result.content().stream().map(OrganizationResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
