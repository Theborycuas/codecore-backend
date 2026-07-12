package com.codecore.appointment.interfaces.http.admin.dto;

import com.codecore.appointment.application.dto.AdminAppointmentView;
import com.codecore.appointment.application.dto.PagedResult;

import java.util.List;

public record PagedAppointmentResponse(
        List<AppointmentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static PagedAppointmentResponse from(PagedResult<AdminAppointmentView> result) {
        return new PagedAppointmentResponse(
                result.content().stream().map(AppointmentResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
