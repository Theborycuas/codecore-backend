package com.codecore.iam.application.admin;

import com.codecore.iam.application.command.UpdateAdminTenantCommand;
import com.codecore.iam.application.dto.AdminTenantView;
import com.codecore.iam.application.port.in.GetAdminTenantUseCase;
import com.codecore.iam.application.port.in.UpdateAdminTenantUseCase;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.exception.TenantAlreadyExistsException;
import com.codecore.iam.domain.exception.TenantNotFoundException;
import com.codecore.iam.domain.model.tenant.Tenant;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantName;
import com.codecore.iam.domain.valueobject.TenantStatus;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Tenant administration for the current JWT tenant (FASE 15.7).
 */
public final class TenantAdministrationUseCaseImpl
        implements GetAdminTenantUseCase, UpdateAdminTenantUseCase {

    private final AuthorizationContextAccessor authorizationContextAccessor;
    private final TenantRepository tenantRepository;
    private final TransactionalOperator transactionalOperator;

    public TenantAdministrationUseCaseImpl(
            AuthorizationContextAccessor authorizationContextAccessor,
            TenantRepository tenantRepository,
            TransactionalOperator transactionalOperator
    ) {
        this.authorizationContextAccessor = Objects.requireNonNull(
                authorizationContextAccessor,
                "authorizationContextAccessor"
        );
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<AdminTenantView> execute() {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadTenant(ctx.tenantId()).map(this::toView));
    }

    @Override
    public Mono<AdminTenantView> execute(UpdateAdminTenantCommand command) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadTenant(ctx.tenantId())
                        .flatMap(tenant -> applyUpdates(tenant, command))
                        .flatMap(tenantRepository::save)
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    private Mono<Tenant> loadTenant(TenantId tenantId) {
        return tenantRepository.findById(tenantId)
                .switchIfEmpty(Mono.error(new TenantNotFoundException("Tenant not found")));
    }

    private Mono<Tenant> applyUpdates(Tenant tenant, UpdateAdminTenantCommand command) {
        boolean hasName = command.name() != null && !command.name().isBlank();
        boolean hasStatus = command.status() != null;
        if (!hasName && !hasStatus) {
            return Mono.error(new InvalidDomainValueException("name or status is required"));
        }

        Mono<Tenant> updated = Mono.just(tenant);
        if (hasName) {
            TenantName newName = TenantName.of(command.name());
            if (!newName.equals(tenant.name())) {
                updated = tenantRepository.existsByName(newName)
                        .flatMap(exists -> {
                            if (exists) {
                                return Mono.error(new TenantAlreadyExistsException(
                                        "Tenant name already exists"));
                            }
                            tenant.rename(newName);
                            return Mono.just(tenant);
                        });
            }
        }
        if (hasStatus) {
            updated = updated.flatMap(current -> applyStatus(current, command.status()));
        }
        return updated;
    }

    private Mono<Tenant> applyStatus(Tenant tenant, TenantStatus status) {
        if (status == TenantStatus.ACTIVE) {
            tenant.activate();
        } else if (status == TenantStatus.SUSPENDED) {
            tenant.suspend();
        } else if (status == TenantStatus.DISABLED) {
            tenant.disable();
        } else {
            return Mono.error(new InvalidDomainValueException("Unsupported tenant status"));
        }
        return Mono.just(tenant);
    }

    private AdminTenantView toView(Tenant tenant) {
        return new AdminTenantView(
                tenant.id(),
                tenant.name().value(),
                tenant.status(),
                tenant.createdAt(),
                tenant.updatedAt()
        );
    }
}
