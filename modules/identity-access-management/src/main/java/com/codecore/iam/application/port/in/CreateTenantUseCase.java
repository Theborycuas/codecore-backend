package com.codecore.iam.application.port.in;

import com.codecore.iam.application.command.CreateTenantCommand;
import com.codecore.iam.application.dto.CreateTenantResponse;
import reactor.core.publisher.Mono;

/**
 * Inbound port: create a new {@link com.codecore.iam.domain.model.tenant.Tenant}.
 */
public interface CreateTenantUseCase {

    Mono<CreateTenantResponse> execute(CreateTenantCommand command);
}
