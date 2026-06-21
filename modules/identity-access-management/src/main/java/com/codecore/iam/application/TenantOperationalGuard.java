package com.codecore.iam.application;

import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.domain.exception.TenantNotFoundException;
import com.codecore.iam.domain.exception.TenantNotOperationalException;
import com.codecore.iam.domain.model.tenant.Tenant;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantStatus;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Single enforcement point for tenant operational status (PASO 15.9.3).
 */
public final class TenantOperationalGuard {

    private final TenantRepository tenantRepository;

    public TenantOperationalGuard(TenantRepository tenantRepository) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
    }

    public Mono<Void> assertOperational(TenantId tenantId) {
        return loadTenant(tenantId)
                .flatMap(tenant -> {
                    if (tenant.status() != TenantStatus.ACTIVE) {
                        return Mono.error(new TenantNotOperationalException(tenant.status()));
                    }
                    return Mono.empty();
                });
    }

    public Mono<Tenant> loadTenant(TenantId tenantId) {
        return tenantRepository.findById(tenantId)
                .switchIfEmpty(Mono.error(new TenantNotFoundException("Tenant not found")));
    }
}
