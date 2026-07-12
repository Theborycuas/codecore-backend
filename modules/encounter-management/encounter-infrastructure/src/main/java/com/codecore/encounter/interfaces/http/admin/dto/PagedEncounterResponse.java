package com.codecore.encounter.interfaces.http.admin.dto;

import com.codecore.encounter.application.dto.AdminEncounterView;
import com.codecore.encounter.application.dto.PagedResult;

import java.util.List;

public record PagedEncounterResponse(
        List<EncounterResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedEncounterResponse from(PagedResult<AdminEncounterView> result) {
        return new PagedEncounterResponse(
                result.content().stream().map(EncounterResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
