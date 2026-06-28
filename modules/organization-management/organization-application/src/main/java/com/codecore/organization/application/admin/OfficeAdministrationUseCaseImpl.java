package com.codecore.organization.application.admin;

import com.codecore.organization.application.command.CreateOfficeCommand;
import com.codecore.organization.application.command.UpdateOfficeCommand;
import com.codecore.organization.application.dto.AdminOfficeView;
import com.codecore.organization.application.dto.PagedResult;
import com.codecore.organization.application.port.in.ActivateOfficeUseCase;
import com.codecore.organization.application.port.in.ArchiveOfficeUseCase;
import com.codecore.organization.application.port.in.CreateOfficeUseCase;
import com.codecore.organization.application.port.in.GetOfficeUseCase;
import com.codecore.organization.application.port.in.ListOfficesUseCase;
import com.codecore.organization.application.port.in.UpdateOfficeUseCase;
import com.codecore.organization.application.port.out.OfficeAdminQueryRepository;
import com.codecore.organization.application.port.out.OfficeQueryPort;
import com.codecore.organization.application.port.out.OfficeRepository;
import com.codecore.organization.application.port.out.OrganizationQueryPort;
import com.codecore.organization.application.port.out.TenantContextAccessor;
import com.codecore.organization.application.query.PageQuery;
import com.codecore.organization.application.query.StructureListFilter;
import com.codecore.organization.domain.exception.InvalidDomainValueException;
import com.codecore.organization.domain.exception.OfficeAlreadyExistsException;
import com.codecore.organization.domain.exception.OfficeNotFoundException;
import com.codecore.organization.domain.exception.OrganizationNotActiveException;
import com.codecore.organization.domain.exception.OrganizationNotFoundException;
import com.codecore.organization.domain.model.office.Office;
import com.codecore.organization.domain.model.organization.Organization;
import com.codecore.organization.domain.valueobject.OfficeCode;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OfficeName;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.OrganizationStatus;
import com.codecore.organization.domain.valueobject.TenantId;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

public final class OfficeAdministrationUseCaseImpl
        implements ListOfficesUseCase,
        GetOfficeUseCase,
        CreateOfficeUseCase,
        UpdateOfficeUseCase,
        ArchiveOfficeUseCase,
        ActivateOfficeUseCase {

    private final TenantContextAccessor tenantContextAccessor;
    private final OfficeAdminQueryRepository officeAdminQueryRepository;
    private final OfficeRepository officeRepository;
    private final OfficeQueryPort officeQueryPort;
    private final OrganizationQueryPort organizationQueryPort;
    private final TransactionalOperator transactionalOperator;

    public OfficeAdministrationUseCaseImpl(
            TenantContextAccessor tenantContextAccessor,
            OfficeAdminQueryRepository officeAdminQueryRepository,
            OfficeRepository officeRepository,
            OfficeQueryPort officeQueryPort,
            OrganizationQueryPort organizationQueryPort,
            TransactionalOperator transactionalOperator
    ) {
        this.tenantContextAccessor = Objects.requireNonNull(tenantContextAccessor, "tenantContextAccessor");
        this.officeAdminQueryRepository = Objects.requireNonNull(
                officeAdminQueryRepository,
                "officeAdminQueryRepository"
        );
        this.officeRepository = Objects.requireNonNull(officeRepository, "officeRepository");
        this.officeQueryPort = Objects.requireNonNull(officeQueryPort, "officeQueryPort");
        this.organizationQueryPort = Objects.requireNonNull(organizationQueryPort, "organizationQueryPort");
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<PagedResult<AdminOfficeView>> execute(
            OrganizationId organizationId,
            StructureListFilter filter,
            PageQuery pageQuery
    ) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> assertOrganizationInTenant(tenantId, organizationId)
                        .then(officeAdminQueryRepository.countByTenantId(tenantId, organizationId, filter))
                        .flatMap(total -> officeAdminQueryRepository
                                .findByTenantId(tenantId, organizationId, filter, pageQuery)
                                .collectList()
                                .map(content -> PagedResult.of(
                                        content,
                                        pageQuery.page(),
                                        pageQuery.size(),
                                        total
                                ))));
    }

    @Override
    public Mono<AdminOfficeView> execute(OfficeId officeId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, officeId).map(this::toView));
    }

    @Override
    public Mono<AdminOfficeView> execute(CreateOfficeCommand command) {
        OrganizationId organizationId = new OrganizationId(command.organizationId());
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadActiveOrganization(tenantId, organizationId)
                        .flatMap(organization -> {
                            OfficeCode code = OfficeCode.of(command.code());
                            OfficeName name = OfficeName.of(command.name());
                            return officeRepository.existsByOrganizationIdAndCode(organizationId, code)
                                    .flatMap(exists -> {
                                        if (exists) {
                                            return Mono.error(new OfficeAlreadyExistsException(
                                                    "Office code already exists in organization"));
                                        }
                                        Instant now = Instant.now();
                                        Office office = Office.create(
                                                OfficeId.generate(),
                                                tenantId,
                                                organizationId,
                                                code,
                                                name,
                                                now
                                        );
                                        return officeRepository.save(office).map(this::toView);
                                    });
                        }))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminOfficeView> execute(UpdateOfficeCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, command.officeId())
                        .flatMap(office -> {
                            if (command.name() == null || command.name().isBlank()) {
                                return Mono.error(new InvalidDomainValueException("name is required"));
                            }
                            office.rename(OfficeName.of(command.name()));
                            return officeRepository.save(office);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminOfficeView> archive(OfficeId officeId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, officeId)
                        .flatMap(office -> {
                            office.archive();
                            return officeRepository.save(office);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminOfficeView> activate(OfficeId officeId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, officeId)
                        .flatMap(office -> loadActiveOrganization(tenantId, office.organizationId())
                                .then(Mono.defer(() -> {
                                    office.activate();
                                    return officeRepository.save(office);
                                })))
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    private Mono<Void> assertOrganizationInTenant(TenantId tenantId, OrganizationId organizationId) {
        return organizationQueryPort.findByIdAndTenantId(organizationId, tenantId)
                .switchIfEmpty(Mono.error(new OrganizationNotFoundException(
                        "Organization not found in tenant context")))
                .then();
    }

    private Mono<Organization> loadActiveOrganization(TenantId tenantId, OrganizationId organizationId) {
        return organizationQueryPort.findByIdAndTenantId(organizationId, tenantId)
                .switchIfEmpty(Mono.error(new OrganizationNotFoundException(
                        "Organization not found in tenant context")))
                .flatMap(organization -> {
                    if (organization.status() != OrganizationStatus.ACTIVE) {
                        return Mono.error(new OrganizationNotActiveException(
                                "Organization is not active"));
                    }
                    return Mono.just(organization);
                });
    }

    private Mono<Office> loadInTenant(TenantId tenantId, OfficeId officeId) {
        return officeQueryPort.findByIdAndTenantId(officeId, tenantId)
                .switchIfEmpty(Mono.error(new OfficeNotFoundException("Office not found in tenant context")));
    }

    private AdminOfficeView toView(Office office) {
        return new AdminOfficeView(
                office.id(),
                office.tenantId(),
                office.organizationId(),
                office.code().value(),
                office.name().value(),
                office.status(),
                office.createdAt(),
                office.updatedAt()
        );
    }
}
