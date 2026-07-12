package com.codecore.inventory.interfaces.http.admin.dto;

import com.codecore.inventory.application.dto.AdminItemView;
import com.codecore.inventory.application.dto.PagedResult;

import java.util.List;

public record PagedItemResponse(
        List<ItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedItemResponse from(PagedResult<AdminItemView> result) {
        return new PagedItemResponse(
                result.content().stream().map(ItemResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
