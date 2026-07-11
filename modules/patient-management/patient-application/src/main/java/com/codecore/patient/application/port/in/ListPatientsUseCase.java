package com.codecore.patient.application.port.in;

import com.codecore.patient.application.dto.AdminPatientView;
import com.codecore.patient.application.dto.PagedResult;
import com.codecore.patient.application.query.PageQuery;
import com.codecore.patient.application.query.PatientListQuery;
import reactor.core.publisher.Mono;

public interface ListPatientsUseCase {

    Mono<PagedResult<AdminPatientView>> execute(PatientListQuery filter, PageQuery pageQuery);
}
