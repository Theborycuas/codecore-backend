package com.codecore.patient.application.port.in;

import com.codecore.patient.application.dto.AdminPatientView;
import com.codecore.patient.domain.valueobject.PatientId;
import reactor.core.publisher.Mono;

public interface GetPatientUseCase {

    Mono<AdminPatientView> execute(PatientId patientId);
}
