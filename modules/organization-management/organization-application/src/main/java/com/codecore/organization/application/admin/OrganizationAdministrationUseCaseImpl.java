package com.codecore.organization.application.admin;

import com.codecore.organization.application.command.CreateOrganizationCommand;
import com.codecore.organization.application.command.UpdateOrganizationCommand;
import com.codecore.organization.application.dto.AdminOrganizationView;
import com.codecore.organization.application.dto.PagedResult;
import com.codecore.organization.application.port.in.ActivateOrganizationUseCase;
import com.codecore.organization.application.port.in.ArchiveOrganizationUseCase;
import com.codecore.organization.application.port.in.CreateOrganizationUseCase;
import com.codecore.organization.application.port.in.GetOrganizationUseCase;
import com.codecore.organization.application.port.in.ListOrganizationsUseCase;
import com.codecore.organization.application.port.in.UpdateOrganizationUseCase;
import com.codecore.organization.application.port.out.OfficeRepository;
import com.codecore.organization.application.port.out.OrganizationAdminQueryRepository;
import com.codecore.organization.application.port.out.OrganizationQueryPort;
import com.codecore.organization.application.port.out.OrganizationRepository;
import com.codecore.organization.application.port.out.TenantContextAccessor;
import com.codecore.organization.application.query.PageQuery;
import com.codecore.organization.application.query.StructureListFilter;
import com.codecore.organization.domain.exception.InvalidDomainValueException;
import com.codecore.organization.domain.exception.OrganizationAlreadyExistsException;
import com.codecore.organization.domain.exception.OrganizationHasActiveOfficesException;
import com.codecore.organization.domain.exception.OrganizationNotFoundException;
import com.codecore.organization.domain.model.organization.Organization;
import com.codecore.organization.domain.valueobject.OrganizationCode;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.OrganizationName;
import com.codecore.organization.domain.valueobject.TenantId;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

public final class OrganizationAdministrationUseCaseImpl
        implements ListOrganizationsUseCase,
        GetOrganizationUseCase,
        CreateOrganizationUseCase,
        UpdateOrganizationUseCase,
        ArchiveOrganizationUseCase,
        ActivateOrganizationUseCase {

    private final TenantContextAccessor tenantContextAccessor;
    private final OrganizationAdminQueryRepository organizationAdminQueryRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationQueryPort organizationQueryPort;
    private final OfficeRepository officeRepository;
    private final TransactionalOperator transactionalOperator;

    public OrganizationAdministrationUseCaseImpl(
            TenantContextAccessor tenantContextAccessor,
            OrganizationAdminQueryRepository organizationAdminQueryRepository,
            OrganizationRepository organizationRepository,
            OrganizationQueryPort organizationQueryPort,
            OfficeRepository officeRepository,
            TransactionalOperator transactionalOperator
    ) {
        this.tenantContextAccessor = Objects.requireNonNull(tenantContextAccessor, "tenantContextAccessor");
        this.organizationAdminQueryRepository = Objects.requireNonNull(
                organizationAdminQueryRepository,
                "organizationAdminQueryRepository"
        );
        this.organizationRepository = Objects.requireNonNull(organizationRepository, "organizationRepository");
        this.organizationQueryPort = Objects.requireNonNull(organizationQueryPort, "organizationQueryPort");
        this.officeRepository = Objects.requireNonNull(officeRepository, "officeRepository");
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<PagedResult<AdminOrganizationView>> execute(StructureListFilter filter, PageQuery pageQuery) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> organizationAdminQueryRepository.countByTenantId(tenantId, filter)
                        .flatMap(total -> organizationAdminQueryRepository
                                .findByTenantId(tenantId, filter, pageQuery)
                                .collectList()
                                .map(content -> PagedResult.of(
                                        content,
                                        pageQuery.page(),
                                        pageQuery.size(),
                                        total
                                ))));
    }

    @Override
    public Mono<AdminOrganizationView> execute(OrganizationId organizationId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, organizationId).map(this::toView));
    }

    @Override
    public Mono<AdminOrganizationView> execute(CreateOrganizationCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> {
                    OrganizationCode code = OrganizationCode.of(command.code());
                    OrganizationName name = OrganizationName.of(command.name());
                    return organizationRepository.existsByTenantIdAndCode(tenantId, code)
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.error(new OrganizationAlreadyExistsException(
                                            "Organization code already exists in tenant"));
                                }
                                Instant now = Instant.now();
                                Organization organization = Organization.create(
                                        OrganizationId.generate(),
                                        tenantId,
                                        code,
                                        name,
                                        now
                                );
                                return organizationRepository.save(organization).map(this::toView);
                            });
                })
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminOrganizationView> execute(UpdateOrganizationCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, command.organizationId())
                        .flatMap(organization -> {
                            if (command.name() == null || command.name().isBlank()) {
                                return Mono.error(new InvalidDomainValueException("name is required"));
                            }
                            organization.rename(OrganizationName.of(command.name()));
                            return organizationRepository.save(organization);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminOrganizationView> archive(OrganizationId organizationId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, organizationId)
                        .flatMap(organization -> officeRepository
                                .countActiveByOrganizationId(organizationId)
                                .flatMap(activeCount -> {
                                    if (activeCount > 0) {
                                        return Mono.error(new OrganizationHasActiveOfficesException(
                                                "Archive offices first"));
                                    }
                                    organization.archive();
                                    return organizationRepository.save(organization);
                                }))
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminOrganizationView> activate(OrganizationId organizationId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, organizationId)
                        .flatMap(organization -> {
                            organization.activate();
                            return organizationRepository.save(organization);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    private Mono<Organization> loadInTenant(TenantId tenantId, OrganizationId organizationId) {
        return organizationQueryPort.findByIdAndTenantId(organizationId, tenantId)
                .switchIfEmpty(Mono.error(new OrganizationNotFoundException(
                        "Organization not found in tenant context")));
    }

    private AdminOrganizationView toView(Organization organization) {
        return new AdminOrganizationView(
                organization.id(),
                organization.tenantId(),
                organization.code().value(),
                organization.name().value(),
                organization.status(),
                organization.createdAt(),
                organization.updatedAt()
        );
    }
}
