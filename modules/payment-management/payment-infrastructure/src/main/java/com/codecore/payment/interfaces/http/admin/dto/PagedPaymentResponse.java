package com.codecore.payment.interfaces.http.admin.dto;

import com.codecore.payment.application.dto.AdminPaymentView;
import com.codecore.payment.application.dto.PagedResult;

import java.util.List;

public record PagedPaymentResponse(
        List<PaymentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedPaymentResponse from(PagedResult<AdminPaymentView> result) {
        return new PagedPaymentResponse(
                result.content().stream().map(PaymentResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
