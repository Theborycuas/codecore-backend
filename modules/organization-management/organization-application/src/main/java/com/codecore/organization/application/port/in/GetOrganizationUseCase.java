package com.codecore.organization.application.port.in;

import com.codecore.organization.application.dto.AdminOrganizationView;
import com.codecore.organization.domain.valueobject.OrganizationId;
import reactor.core.publisher.Mono;

public interface GetOrganizationUseCase {

    Mono<AdminOrganizationView> execute(OrganizationId organizationId);
}
