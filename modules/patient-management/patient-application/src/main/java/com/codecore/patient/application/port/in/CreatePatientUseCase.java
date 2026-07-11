package com.codecore.patient.application.port.in;

import com.codecore.patient.application.command.CreatePatientCommand;
import com.codecore.patient.application.dto.AdminPatientView;
import reactor.core.publisher.Mono;

public interface CreatePatientUseCase {

    Mono<AdminPatientView> execute(CreatePatientCommand command);
}
