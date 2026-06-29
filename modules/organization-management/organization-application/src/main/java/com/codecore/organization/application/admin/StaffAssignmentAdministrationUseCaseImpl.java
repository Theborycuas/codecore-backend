package com.codecore.organization.application.admin;

import com.codecore.organization.application.command.CreateStaffAssignmentCommand;
import com.codecore.organization.application.command.UpdateStaffAssignmentCommand;
import com.codecore.organization.application.dto.AdminStaffAssignmentView;
import com.codecore.organization.application.dto.PagedResult;
import com.codecore.organization.application.port.in.CreateStaffAssignmentUseCase;
import com.codecore.organization.application.port.in.DeleteStaffAssignmentUseCase;
import com.codecore.organization.application.port.in.GetStaffAssignmentUseCase;
import com.codecore.organization.application.port.in.ListStaffAssignmentsUseCase;
import com.codecore.organization.application.port.in.UpdateStaffAssignmentUseCase;
import com.codecore.organization.application.port.out.MembershipReferencePort;
import com.codecore.organization.application.port.out.OfficeQueryPort;
import com.codecore.organization.application.port.out.OrganizationQueryPort;
import com.codecore.organization.application.port.out.StaffAssignmentAdminQueryRepository;
import com.codecore.organization.application.port.out.StaffAssignmentQueryPort;
import com.codecore.organization.application.port.out.StaffAssignmentRepository;
import com.codecore.organization.application.port.out.TenantContextAccessor;
import com.codecore.organization.application.query.PageQuery;
import com.codecore.organization.application.query.StaffAssignmentListFilter;
import com.codecore.organization.domain.exception.MembershipNotInTenantException;
import com.codecore.organization.domain.exception.OfficeNotFoundException;
import com.codecore.organization.domain.exception.OrganizationNotActiveException;
import com.codecore.organization.domain.exception.OrganizationNotFoundException;
import com.codecore.organization.domain.exception.StaffAssignmentAlreadyExistsException;
import com.codecore.organization.domain.exception.StaffAssignmentNotFoundException;
import com.codecore.organization.domain.model.organization.Organization;
import com.codecore.organization.domain.model.staffassignment.StaffAssignment;
import com.codecore.organization.domain.valueobject.MembershipId;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OfficeStatus;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.OrganizationStatus;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import com.codecore.organization.domain.valueobject.TenantId;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

public final class StaffAssignmentAdministrationUseCaseImpl
        implements ListStaffAssignmentsUseCase,
        GetStaffAssignmentUseCase,
        CreateStaffAssignmentUseCase,
        UpdateStaffAssignmentUseCase,
        DeleteStaffAssignmentUseCase {

    private final TenantContextAccessor tenantContextAccessor;
    private final StaffAssignmentAdminQueryRepository staffAssignmentAdminQueryRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final StaffAssignmentQueryPort staffAssignmentQueryPort;
    private final OrganizationQueryPort organizationQueryPort;
    private final OfficeQueryPort officeQueryPort;
    private final MembershipReferencePort membershipReferencePort;
    private final TransactionalOperator transactionalOperator;

    public StaffAssignmentAdministrationUseCaseImpl(
            TenantContextAccessor tenantContextAccessor,
            StaffAssignmentAdminQueryRepository staffAssignmentAdminQueryRepository,
            StaffAssignmentRepository staffAssignmentRepository,
            StaffAssignmentQueryPort staffAssignmentQueryPort,
            OrganizationQueryPort organizationQueryPort,
            OfficeQueryPort officeQueryPort,
            MembershipReferencePort membershipReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        this.tenantContextAccessor = Objects.requireNonNull(tenantContextAccessor, "tenantContextAccessor");
        this.staffAssignmentAdminQueryRepository = Objects.requireNonNull(
                staffAssignmentAdminQueryRepository,
                "staffAssignmentAdminQueryRepository"
        );
        this.staffAssignmentRepository = Objects.requireNonNull(staffAssignmentRepository, "staffAssignmentRepository");
        this.staffAssignmentQueryPort = Objects.requireNonNull(staffAssignmentQueryPort, "staffAssignmentQueryPort");
        this.organizationQueryPort = Objects.requireNonNull(organizationQueryPort, "organizationQueryPort");
        this.officeQueryPort = Objects.requireNonNull(officeQueryPort, "officeQueryPort");
        this.membershipReferencePort = Objects.requireNonNull(membershipReferencePort, "membershipReferencePort");
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<PagedResult<AdminStaffAssignmentView>> execute(
            StaffAssignmentListFilter filter,
            PageQuery pageQuery
    ) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> staffAssignmentAdminQueryRepository.countByTenantId(tenantId, filter)
                        .flatMap(total -> staffAssignmentAdminQueryRepository
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
    public Mono<AdminStaffAssignmentView> execute(StaffAssignmentId assignmentId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, assignmentId).map(this::toView));
    }

    @Override
    public Mono<AdminStaffAssignmentView> execute(CreateStaffAssignmentCommand command) {
        OrganizationId organizationId = new OrganizationId(command.organizationId());
        OfficeId officeId = command.officeId() != null ? new OfficeId(command.officeId()) : null;
        MembershipId membershipId = new MembershipId(command.membershipId());

        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> assertActiveMembership(tenantId, membershipId)
                        .then(validateScopeTargets(tenantId, organizationId, officeId))
                        .then(staffAssignmentRepository.existsByScope(tenantId, membershipId, organizationId, officeId))
                        .flatMap(exists -> {
                            if (exists) {
                                return Mono.error(new StaffAssignmentAlreadyExistsException(
                                        "Staff assignment already exists for this scope"));
                            }
                            Instant now = Instant.now();
                            StaffAssignment assignment = StaffAssignment.create(
                                    StaffAssignmentId.generate(),
                                    tenantId,
                                    membershipId,
                                    organizationId,
                                    officeId,
                                    now
                            );
                            return staffAssignmentRepository.save(assignment);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminStaffAssignmentView> execute(UpdateStaffAssignmentCommand command) {
        OrganizationId organizationId = new OrganizationId(command.organizationId());
        OfficeId officeId = command.officeId() != null ? new OfficeId(command.officeId()) : null;

        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, command.assignmentId())
                        .flatMap(assignment -> validateScopeTargets(tenantId, organizationId, officeId)
                                .then(staffAssignmentRepository.existsByScope(
                                        tenantId,
                                        assignment.membershipId(),
                                        organizationId,
                                        officeId
                                ))
                                .flatMap(exists -> {
                                    boolean sameScope = assignment.organizationId().equals(organizationId)
                                            && Objects.equals(assignment.officeId(), officeId);
                                    if (exists && !sameScope) {
                                        return Mono.error(new StaffAssignmentAlreadyExistsException(
                                                "Staff assignment already exists for this scope"));
                                    }
                                    assignment.changeScope(organizationId, officeId);
                                    return staffAssignmentRepository.save(assignment);
                                }))
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Void> delete(StaffAssignmentId assignmentId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, assignmentId)
                        .flatMap(assignment -> staffAssignmentRepository.delete(assignment.id())))
                .as(transactionalOperator::transactional);
    }

    private Mono<Void> assertActiveMembership(TenantId tenantId, MembershipId membershipId) {
        return membershipReferencePort.existsActiveByIdAndTenant(membershipId, tenantId)
                .flatMap(exists -> exists
                        ? Mono.empty()
                        : Mono.error(new MembershipNotInTenantException(
                                "Active membership not found in tenant context")));
    }

    private Mono<Void> validateScopeTargets(
            TenantId tenantId,
            OrganizationId organizationId,
            OfficeId officeId
    ) {
        return loadActiveOrganization(tenantId, organizationId)
                .flatMap(organization -> {
                    if (officeId == null) {
                        return Mono.empty();
                    }
                    return officeQueryPort.findByIdAndTenantId(officeId, tenantId)
                            .switchIfEmpty(Mono.error(new OfficeNotFoundException(
                                    "Office not found in tenant context")))
                            .flatMap(office -> {
                                if (office.status() != OfficeStatus.ACTIVE) {
                                    return Mono.error(new OrganizationNotActiveException(
                                            "Office is not active"));
                                }
                                StaffAssignment.assertOfficeBelongsToOrganization(
                                        organizationId,
                                        officeId,
                                        office.organizationId()
                                );
                                return Mono.empty();
                            });
                });
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

    private Mono<StaffAssignment> loadInTenant(TenantId tenantId, StaffAssignmentId assignmentId) {
        return staffAssignmentQueryPort.findByIdAndTenantId(assignmentId, tenantId)
                .switchIfEmpty(Mono.error(new StaffAssignmentNotFoundException(
                        "Staff assignment not found in tenant context")));
    }

    private AdminStaffAssignmentView toView(StaffAssignment assignment) {
        return new AdminStaffAssignmentView(
                assignment.id(),
                assignment.tenantId(),
                assignment.membershipId(),
                assignment.organizationId(),
                assignment.officeId(),
                assignment.createdAt(),
                assignment.updatedAt()
        );
    }
}
