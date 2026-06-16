package com.codecore.iam.application;

import com.codecore.iam.application.command.CreateTenantCommand;
import com.codecore.iam.application.dto.CreateTenantResponse;
import com.codecore.iam.application.port.in.CreateTenantUseCase;
import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.application.port.out.TenantSystemRolesProvisioner;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.exception.TenantAlreadyExistsException;
import com.codecore.iam.domain.model.tenant.Tenant;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantName;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Creates a new {@link Tenant} with unique name (no HTTP/JWT).
 */
public class CreateTenantUseCaseImpl implements CreateTenantUseCase {

    private final TenantRepository tenantRepository;
    private final TenantSystemRolesProvisioner tenantSystemRolesProvisioner;

    public CreateTenantUseCaseImpl(
            TenantRepository tenantRepository,
            TenantSystemRolesProvisioner tenantSystemRolesProvisioner
    ) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.tenantSystemRolesProvisioner = Objects.requireNonNull(
                tenantSystemRolesProvisioner,
                "tenantSystemRolesProvisioner"
        );
    }

    @Override
    public Mono<CreateTenantResponse> execute(CreateTenantCommand command) {
        return Mono.defer(() -> {
            Objects.requireNonNull(command, "command");
            validateNotBlank(command.name(), "Name must not be blank");

            TenantName name = TenantName.of(command.name());

            return tenantRepository.existsByName(name)
                    .flatMap(exists -> {
                        if (exists) {
                            return Mono.error(new TenantAlreadyExistsException(
                                    "Tenant already exists with this name"));
                        }
                        return createAndPersist(name);
                    });
        });
    }

    private Mono<CreateTenantResponse> createAndPersist(TenantName name) {
        TenantId tenantId = TenantId.generate();
        Instant now = Instant.now();
        Tenant tenant = Tenant.create(tenantId, name, now);

        return tenantRepository.save(tenant)
                .flatMap(saved -> tenantSystemRolesProvisioner.provisionForTenant(saved.id()).thenReturn(saved))
                .map(saved -> new CreateTenantResponse(
                        saved.id(),
                        saved.name(),
                        saved.status()
                ));
    }

    private static void validateNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainValueException(message);
        }
    }
}
