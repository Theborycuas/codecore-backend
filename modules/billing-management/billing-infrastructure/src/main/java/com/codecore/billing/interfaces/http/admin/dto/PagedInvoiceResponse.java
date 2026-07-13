package com.codecore.billing.interfaces.http.admin.dto;

import com.codecore.billing.application.dto.AdminInvoiceView;
import com.codecore.billing.application.dto.PagedResult;

import java.util.List;

public record PagedInvoiceResponse(
        List<InvoiceResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedInvoiceResponse from(PagedResult<AdminInvoiceView> result) {
        return new PagedInvoiceResponse(
                result.content().stream().map(InvoiceResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
