package com.codecore.organization.application.port.in;

import com.codecore.organization.application.dto.AdminOfficeView;
import com.codecore.organization.domain.valueobject.OfficeId;
import reactor.core.publisher.Mono;

public interface GetOfficeUseCase {

    Mono<AdminOfficeView> execute(OfficeId officeId);
}
