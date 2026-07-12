package com.codecore.appointment.application.port.in;

import com.codecore.appointment.application.command.CreateAppointmentCommand;
import com.codecore.appointment.application.dto.AdminAppointmentView;
import reactor.core.publisher.Mono;

public interface CreateAppointmentUseCase {

    Mono<AdminAppointmentView> execute(CreateAppointmentCommand command);
}
