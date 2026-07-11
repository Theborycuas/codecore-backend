package com.codecore.patient.application.port.in;

import com.codecore.patient.application.command.UpdatePatientCommand;
import com.codecore.patient.application.dto.AdminPatientView;
import reactor.core.publisher.Mono;

public interface UpdatePatientUseCase {

    Mono<AdminPatientView> execute(UpdatePatientCommand command);
}
