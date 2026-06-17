package com.codecore.iam.application.port.in;

import com.codecore.iam.application.dto.AdminTenantView;
import reactor.core.publisher.Mono;

public interface GetAdminTenantUseCase {

    Mono<AdminTenantView> execute();
}
