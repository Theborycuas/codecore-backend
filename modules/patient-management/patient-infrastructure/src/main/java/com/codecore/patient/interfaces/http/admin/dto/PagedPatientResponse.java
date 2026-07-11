package com.codecore.patient.interfaces.http.admin.dto;

import com.codecore.patient.application.dto.AdminPatientView;
import com.codecore.patient.application.dto.PagedResult;

import java.util.List;

public record PagedPatientResponse(
        List<PatientResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedPatientResponse from(PagedResult<AdminPatientView> result) {
        return new PagedPatientResponse(
                result.content().stream().map(PatientResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
