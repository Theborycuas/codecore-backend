package com.codecore.appointment.application.port.in;

import com.codecore.appointment.application.dto.AdminAppointmentView;
import com.codecore.appointment.application.dto.PagedResult;
import com.codecore.appointment.application.query.AppointmentListQuery;
import com.codecore.appointment.application.query.PageQuery;
import reactor.core.publisher.Mono;

public interface ListAppointmentsUseCase {

    Mono<PagedResult<AdminAppointmentView>> execute(AppointmentListQuery filter, PageQuery pageQuery);
}
