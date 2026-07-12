package com.codecore.appointment.application.admin;

import com.codecore.appointment.application.command.CreateAppointmentCommand;
import com.codecore.appointment.application.command.UpdateAppointmentCommand;
import com.codecore.appointment.application.dto.AdminAppointmentView;
import com.codecore.appointment.application.dto.PagedResult;
import com.codecore.appointment.application.port.in.CancelAppointmentUseCase;
import com.codecore.appointment.application.port.in.CompleteAppointmentUseCase;
import com.codecore.appointment.application.port.in.CreateAppointmentUseCase;
import com.codecore.appointment.application.port.in.GetAppointmentUseCase;
import com.codecore.appointment.application.port.in.ListAppointmentsUseCase;
import com.codecore.appointment.application.port.in.UpdateAppointmentUseCase;
import com.codecore.appointment.application.port.out.AppointmentAdminQueryRepository;
import com.codecore.appointment.application.port.out.AppointmentQueryPort;
import com.codecore.appointment.application.port.out.AppointmentRepository;
import com.codecore.appointment.application.port.out.TenantContextAccessor;
import com.codecore.appointment.application.query.AppointmentListQuery;
import com.codecore.appointment.application.query.PageQuery;
import com.codecore.appointment.domain.exception.AppointmentCoherenceException;
import com.codecore.appointment.domain.exception.AppointmentNotFoundException;
import com.codecore.appointment.domain.exception.AppointmentReferenceNotFoundException;
import com.codecore.appointment.domain.exception.InvalidDomainValueException;
import com.codecore.appointment.domain.model.appointment.Appointment;
import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.AppointmentTimeWindow;
import com.codecore.appointment.domain.valueobject.OfficeId;
import com.codecore.appointment.domain.valueobject.OrganizationId;
import com.codecore.appointment.domain.valueobject.PatientId;
import com.codecore.appointment.domain.valueobject.StaffAssignmentId;
import com.codecore.appointment.domain.valueobject.TenantId;
import com.codecore.organization.contract.reference.OfficeReferencePort;
import com.codecore.organization.contract.reference.OrganizationReferencePort;
import com.codecore.organization.contract.reference.StaffAssignmentReferencePort;
import com.codecore.organization.contract.reference.StaffAssignmentReferenceView;
import com.codecore.patient.contract.reference.PatientReferencePort;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Appointment administration use cases (PASO 18.6) — multi-ReferencePort write validation (ADR-013 / ADR-014).
 */
public final class AppointmentAdministrationUseCaseImpl
        implements ListAppointmentsUseCase,
        GetAppointmentUseCase,
        CreateAppointmentUseCase,
        UpdateAppointmentUseCase,
        CancelAppointmentUseCase,
        CompleteAppointmentUseCase {

    private final TenantContextAccessor tenantContextAccessor;
    private final AppointmentAdminQueryRepository appointmentAdminQueryRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentQueryPort appointmentQueryPort;
    private final PatientReferencePort patientReferencePort;
    private final OrganizationReferencePort organizationReferencePort;
    private final OfficeReferencePort officeReferencePort;
    private final StaffAssignmentReferencePort staffAssignmentReferencePort;
    private final TransactionalOperator transactionalOperator;

    public AppointmentAdministrationUseCaseImpl(
            TenantContextAccessor tenantContextAccessor,
            AppointmentAdminQueryRepository appointmentAdminQueryRepository,
            AppointmentRepository appointmentRepository,
            AppointmentQueryPort appointmentQueryPort,
            PatientReferencePort patientReferencePort,
            OrganizationReferencePort organizationReferencePort,
            OfficeReferencePort officeReferencePort,
            StaffAssignmentReferencePort staffAssignmentReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        this.tenantContextAccessor = Objects.requireNonNull(tenantContextAccessor, "tenantContextAccessor");
        this.appointmentAdminQueryRepository = Objects.requireNonNull(
                appointmentAdminQueryRepository,
                "appointmentAdminQueryRepository"
        );
        this.appointmentRepository = Objects.requireNonNull(appointmentRepository, "appointmentRepository");
        this.appointmentQueryPort = Objects.requireNonNull(appointmentQueryPort, "appointmentQueryPort");
        this.patientReferencePort = Objects.requireNonNull(patientReferencePort, "patientReferencePort");
        this.organizationReferencePort = Objects.requireNonNull(
                organizationReferencePort,
                "organizationReferencePort"
        );
        this.officeReferencePort = Objects.requireNonNull(officeReferencePort, "officeReferencePort");
        this.staffAssignmentReferencePort = Objects.requireNonNull(
                staffAssignmentReferencePort,
                "staffAssignmentReferencePort"
        );
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<PagedResult<AdminAppointmentView>> execute(AppointmentListQuery filter, PageQuery pageQuery) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> appointmentAdminQueryRepository.countByTenantId(tenantId, filter)
                        .flatMap(total -> appointmentAdminQueryRepository
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
    public Mono<AdminAppointmentView> execute(AppointmentId appointmentId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, appointmentId).map(this::toView));
    }

    @Override
    public Mono<AdminAppointmentView> execute(CreateAppointmentCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> validateWriteRefs(
                        tenantId,
                        command.patientId(),
                        command.staffAssignmentId(),
                        command.organizationId(),
                        command.officeId()
                ).then(Mono.defer(() -> {
                    AppointmentTimeWindow window = requireTimeWindow(command.startsAt(), command.endsAt());
                    Appointment appointment = Appointment.schedule(
                            AppointmentId.generate(),
                            tenantId,
                            PatientId.of(command.patientId()),
                            StaffAssignmentId.of(command.staffAssignmentId()),
                            OrganizationId.of(command.organizationId()),
                            command.officeId() == null ? null : OfficeId.of(command.officeId()),
                            window,
                            Instant.now()
                    );
                    return appointmentRepository.save(appointment).map(this::toView);
                })))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminAppointmentView> execute(UpdateAppointmentCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, command.appointmentId())
                        .flatMap(appointment -> validateWriteRefs(
                                tenantId,
                                command.patientId(),
                                command.staffAssignmentId(),
                                command.organizationId(),
                                command.officeId()
                        ).then(Mono.defer(() -> {
                            AppointmentTimeWindow window = requireTimeWindow(command.startsAt(), command.endsAt());
                            appointment.changePatient(PatientId.of(command.patientId()));
                            appointment.changeStaffAssignment(StaffAssignmentId.of(command.staffAssignmentId()));
                            appointment.changeOrganization(OrganizationId.of(command.organizationId()));
                            if (command.officeId() == null) {
                                appointment.clearOffice();
                            } else {
                                appointment.assignOffice(OfficeId.of(command.officeId()));
                            }
                            appointment.reschedule(window);
                            return appointmentRepository.save(appointment);
                        })))
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminAppointmentView> cancel(AppointmentId appointmentId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, appointmentId)
                        .flatMap(appointment -> {
                            appointment.cancel();
                            return appointmentRepository.save(appointment);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminAppointmentView> complete(AppointmentId appointmentId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, appointmentId)
                        .flatMap(appointment -> {
                            appointment.complete();
                            return appointmentRepository.save(appointment);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    private Mono<Appointment> loadInTenant(TenantId tenantId, AppointmentId appointmentId) {
        return appointmentQueryPort.findByIdAndTenantId(appointmentId, tenantId)
                .switchIfEmpty(Mono.error(new AppointmentNotFoundException(
                        "Appointment not found in tenant context")));
    }

    /**
     * Write-time ReferencePort + coherence validation (PASO 18.5.1 §8). Cancel/complete skip this.
     */
    private Mono<Void> validateWriteRefs(
            TenantId tenantId,
            UUID patientId,
            UUID staffAssignmentId,
            UUID organizationId,
            UUID officeId
    ) {
        if (patientId == null || staffAssignmentId == null || organizationId == null) {
            return Mono.error(new InvalidDomainValueException(
                    "patientId, staffAssignmentId and organizationId are required"));
        }

        com.codecore.patient.domain.valueobject.PatientId patientRef =
                new com.codecore.patient.domain.valueobject.PatientId(patientId);
        com.codecore.patient.domain.valueobject.TenantId patientTenant =
                new com.codecore.patient.domain.valueobject.TenantId(tenantId.value());

        com.codecore.organization.domain.valueobject.OrganizationId orgRef =
                new com.codecore.organization.domain.valueobject.OrganizationId(organizationId);
        com.codecore.organization.domain.valueobject.TenantId orgTenant =
                new com.codecore.organization.domain.valueobject.TenantId(tenantId.value());
        com.codecore.organization.domain.valueobject.StaffAssignmentId staffRef =
                new com.codecore.organization.domain.valueobject.StaffAssignmentId(staffAssignmentId);

        return patientReferencePort.existsActiveByIdAndTenant(patientRef, patientTenant)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new AppointmentReferenceNotFoundException(
                                "Patient not found or not ACTIVE in tenant"));
                    }
                    return organizationReferencePort.existsActiveByIdAndTenant(orgRef, orgTenant);
                })
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new AppointmentReferenceNotFoundException(
                                "Organization not found or not ACTIVE in tenant"));
                    }
                    return staffAssignmentReferencePort.findScopeByIdAndTenant(staffRef, orgTenant);
                })
                .flatMap(optionalView -> {
                    if (optionalView.isEmpty()) {
                        return Mono.error(new AppointmentReferenceNotFoundException(
                                "StaffAssignment not found in tenant"));
                    }
                    return validateCoherence(optionalView.get(), organizationId, officeId, orgTenant);
                });
    }

    private Mono<Void> validateCoherence(
            StaffAssignmentReferenceView view,
            UUID appointmentOrganizationId,
            UUID appointmentOfficeId,
            com.codecore.organization.domain.valueobject.TenantId orgTenant
    ) {
        if (!view.organizationId().value().equals(appointmentOrganizationId)) {
            return Mono.error(new AppointmentCoherenceException(
                    "Appointment organizationId must equal StaffAssignment organizationId"));
        }

        if (view.officeId().isPresent()) {
            UUID assignmentOfficeId = view.officeId().get().value();
            if (appointmentOfficeId == null || !assignmentOfficeId.equals(appointmentOfficeId)) {
                return Mono.error(new AppointmentCoherenceException(
                        "Appointment officeId must equal StaffAssignment officeId when assignment is office-bound"));
            }
            return Mono.empty();
        }

        // Org-wide assignment: office optional; if present must be ACTIVE in organization.
        if (appointmentOfficeId == null) {
            return Mono.empty();
        }

        com.codecore.organization.domain.valueobject.OfficeId officeRef =
                new com.codecore.organization.domain.valueobject.OfficeId(appointmentOfficeId);
        return officeReferencePort.existsActiveInOrganization(
                        officeRef,
                        view.organizationId(),
                        orgTenant
                )
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new AppointmentReferenceNotFoundException(
                                "Office not found, not ACTIVE, or not in appointment organization"));
                    }
                    return Mono.empty();
                });
    }

    private static AppointmentTimeWindow requireTimeWindow(Instant startsAt, Instant endsAt) {
        if (startsAt == null || endsAt == null) {
            throw new InvalidDomainValueException("startsAt and endsAt are required");
        }
        return AppointmentTimeWindow.of(startsAt, endsAt);
    }

    private AdminAppointmentView toView(Appointment appointment) {
        return new AdminAppointmentView(
                appointment.id(),
                appointment.tenantId(),
                appointment.patientId(),
                appointment.staffAssignmentId(),
                appointment.organizationId(),
                appointment.officeId().orElse(null),
                appointment.startsAt(),
                appointment.endsAt(),
                appointment.status(),
                appointment.createdAt(),
                appointment.updatedAt()
        );
    }
}
