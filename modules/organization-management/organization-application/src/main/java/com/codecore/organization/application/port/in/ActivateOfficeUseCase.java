package com.codecore.organization.application.port.in;

import com.codecore.organization.application.dto.AdminOfficeView;
import com.codecore.organization.domain.valueobject.OfficeId;
import reactor.core.publisher.Mono;

public interface ActivateOfficeUseCase {

    Mono<AdminOfficeView> activate(OfficeId officeId);
}
