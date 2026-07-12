package com.codecore.appointment.application.port.in;

import com.codecore.appointment.application.dto.AdminAppointmentView;
import com.codecore.appointment.domain.valueobject.AppointmentId;
import reactor.core.publisher.Mono;

public interface CancelAppointmentUseCase {

    Mono<AdminAppointmentView> cancel(AppointmentId appointmentId);
}
