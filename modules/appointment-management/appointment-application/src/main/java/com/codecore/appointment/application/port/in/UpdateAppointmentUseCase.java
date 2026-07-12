package com.codecore.appointment.application.port.in;

import com.codecore.appointment.application.command.UpdateAppointmentCommand;
import com.codecore.appointment.application.dto.AdminAppointmentView;
import reactor.core.publisher.Mono;

public interface UpdateAppointmentUseCase {

    Mono<AdminAppointmentView> execute(UpdateAppointmentCommand command);
}
