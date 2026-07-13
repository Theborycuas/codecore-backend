package com.codecore.audit.interfaces.http.admin.dto;

import com.codecore.audit.application.dto.AdminAuditView;
import com.codecore.audit.application.dto.PagedResult;

import java.util.List;

public record PagedAuditEntryResponse(
        List<AuditEntryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedAuditEntryResponse from(PagedResult<AdminAuditView> result) {
        return new PagedAuditEntryResponse(
                result.content().stream().map(AuditEntryResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
