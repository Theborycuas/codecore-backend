package com.codecore.encounter.application.admin;

import com.codecore.appointment.contract.reference.AppointmentReferencePort;
import com.codecore.encounter.application.command.CreateEncounterCommand;
import com.codecore.encounter.application.command.UpdateEncounterCommand;
import com.codecore.encounter.application.dto.AdminEncounterView;
import com.codecore.encounter.application.dto.PagedResult;
import com.codecore.encounter.application.port.in.CancelEncounterUseCase;
import com.codecore.encounter.application.port.in.CompleteEncounterUseCase;
import com.codecore.encounter.application.port.in.CreateEncounterUseCase;
import com.codecore.encounter.application.port.in.GetEncounterUseCase;
import com.codecore.encounter.application.port.in.ListEncountersUseCase;
import com.codecore.encounter.application.port.in.UpdateEncounterUseCase;
import com.codecore.encounter.application.port.out.EncounterAdminQueryRepository;
import com.codecore.encounter.application.port.out.EncounterQueryPort;
import com.codecore.encounter.application.port.out.EncounterRepository;
import com.codecore.encounter.application.port.out.TenantContextAccessor;
import com.codecore.encounter.application.query.EncounterListQuery;
import com.codecore.encounter.application.query.PageQuery;
import com.codecore.encounter.domain.exception.EncounterCoherenceException;
import com.codecore.encounter.domain.exception.EncounterNotFoundException;
import com.codecore.encounter.domain.exception.EncounterReferenceNotFoundException;
import com.codecore.encounter.domain.exception.InvalidDomainValueException;
import com.codecore.encounter.domain.model.encounter.Encounter;
import com.codecore.encounter.domain.valueobject.AppointmentId;
import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.OfficeId;
import com.codecore.encounter.domain.valueobject.OrganizationId;
import com.codecore.encounter.domain.valueobject.PatientId;
import com.codecore.encounter.domain.valueobject.StaffAssignmentId;
import com.codecore.encounter.domain.valueobject.TenantId;
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
 * Encounter administration use cases (PASO 19.6) — multi-ReferencePort write validation (ADR-013 / ADR-015).
 */
public final class EncounterAdministrationUseCaseImpl
        implements ListEncountersUseCase,
        GetEncounterUseCase,
        CreateEncounterUseCase,
        UpdateEncounterUseCase,
        CancelEncounterUseCase,
        CompleteEncounterUseCase {

    private final TenantContextAccessor tenantContextAccessor;
    private final EncounterAdminQueryRepository encounterAdminQueryRepository;
    private final EncounterRepository encounterRepository;
    private final EncounterQueryPort encounterQueryPort;
    private final PatientReferencePort patientReferencePort;
    private final OrganizationReferencePort organizationReferencePort;
    private final OfficeReferencePort officeReferencePort;
    private final StaffAssignmentReferencePort staffAssignmentReferencePort;
    private final AppointmentReferencePort appointmentReferencePort;
    private final TransactionalOperator transactionalOperator;

    public EncounterAdministrationUseCaseImpl(
            TenantContextAccessor tenantContextAccessor,
            EncounterAdminQueryRepository encounterAdminQueryRepository,
            EncounterRepository encounterRepository,
            EncounterQueryPort encounterQueryPort,
            PatientReferencePort patientReferencePort,
            OrganizationReferencePort organizationReferencePort,
            OfficeReferencePort officeReferencePort,
            StaffAssignmentReferencePort staffAssignmentReferencePort,
            AppointmentReferencePort appointmentReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        this.tenantContextAccessor = Objects.requireNonNull(tenantContextAccessor, "tenantContextAccessor");
        this.encounterAdminQueryRepository = Objects.requireNonNull(
                encounterAdminQueryRepository,
                "encounterAdminQueryRepository"
        );
        this.encounterRepository = Objects.requireNonNull(encounterRepository, "encounterRepository");
        this.encounterQueryPort = Objects.requireNonNull(encounterQueryPort, "encounterQueryPort");
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
        this.appointmentReferencePort = Objects.requireNonNull(
                appointmentReferencePort,
                "appointmentReferencePort"
        );
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<PagedResult<AdminEncounterView>> execute(EncounterListQuery filter, PageQuery pageQuery) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> encounterAdminQueryRepository.countByTenantId(tenantId, filter)
                        .flatMap(total -> encounterAdminQueryRepository
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
    public Mono<AdminEncounterView> execute(EncounterId encounterId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, encounterId).map(this::toView));
    }

    @Override
    public Mono<AdminEncounterView> execute(CreateEncounterCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> validateWriteRefs(
                        tenantId,
                        command.patientId(),
                        command.staffAssignmentId(),
                        command.organizationId(),
                        command.officeId(),
                        command.appointmentId()
                ).then(Mono.defer(() -> {
                    Instant startedAt = requireStartedAt(command.startedAt());
                    Encounter encounter = Encounter.open(
                            EncounterId.generate(),
                            tenantId,
                            PatientId.of(command.patientId()),
                            StaffAssignmentId.of(command.staffAssignmentId()),
                            OrganizationId.of(command.organizationId()),
                            command.officeId() == null ? null : OfficeId.of(command.officeId()),
                            command.appointmentId() == null ? null : AppointmentId.of(command.appointmentId()),
                            startedAt,
                            Instant.now()
                    );
                    if (command.endedAt() != null) {
                        encounter.assignEndedAt(command.endedAt());
                    }
                    return encounterRepository.save(encounter).map(this::toView);
                })))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminEncounterView> execute(UpdateEncounterCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, command.encounterId())
                        .flatMap(encounter -> validateWriteRefs(
                                tenantId,
                                command.patientId(),
                                command.staffAssignmentId(),
                                command.organizationId(),
                                command.officeId(),
                                command.appointmentId()
                        ).then(Mono.defer(() -> {
                            Instant startedAt = requireStartedAt(command.startedAt());
                            encounter.changePatient(PatientId.of(command.patientId()));
                            encounter.changeStaffAssignment(StaffAssignmentId.of(command.staffAssignmentId()));
                            encounter.changeOrganization(OrganizationId.of(command.organizationId()));
                            if (command.officeId() == null) {
                                encounter.clearOffice();
                            } else {
                                encounter.assignOffice(OfficeId.of(command.officeId()));
                            }
                            if (command.appointmentId() == null) {
                                encounter.clearAppointment();
                            } else {
                                encounter.linkAppointment(AppointmentId.of(command.appointmentId()));
                            }
                            encounter.changeStartedAt(startedAt);
                            if (command.endedAt() == null) {
                                encounter.clearEndedAt();
                            } else {
                                encounter.assignEndedAt(command.endedAt());
                            }
                            return encounterRepository.save(encounter);
                        })))
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminEncounterView> cancel(EncounterId encounterId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, encounterId)
                        .flatMap(encounter -> {
                            encounter.cancel();
                            return encounterRepository.save(encounter);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminEncounterView> complete(EncounterId encounterId, Instant endedAt) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, encounterId)
                        .flatMap(encounter -> {
                            Instant resolved = resolveEndedAt(endedAt, encounter);
                            encounter.complete(resolved);
                            return encounterRepository.save(encounter);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    private Mono<Encounter> loadInTenant(TenantId tenantId, EncounterId encounterId) {
        return encounterQueryPort.findByIdAndTenantId(encounterId, tenantId)
                .switchIfEmpty(Mono.error(new EncounterNotFoundException(
                        "Encounter not found in tenant context")));
    }

    /**
     * Write-time ReferencePort + coherence validation (PASO 19.5.1 §8). Cancel/complete skip this.
     */
    private Mono<Void> validateWriteRefs(
            TenantId tenantId,
            UUID patientId,
            UUID staffAssignmentId,
            UUID organizationId,
            UUID officeId,
            UUID appointmentId
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
                        return Mono.error(new EncounterReferenceNotFoundException(
                                "Patient not found or not ACTIVE in tenant"));
                    }
                    return organizationReferencePort.existsActiveByIdAndTenant(orgRef, orgTenant);
                })
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new EncounterReferenceNotFoundException(
                                "Organization not found or not ACTIVE in tenant"));
                    }
                    return staffAssignmentReferencePort.findScopeByIdAndTenant(staffRef, orgTenant);
                })
                .flatMap(optionalView -> {
                    if (optionalView.isEmpty()) {
                        return Mono.error(new EncounterReferenceNotFoundException(
                                "StaffAssignment not found in tenant"));
                    }
                    return validateCoherence(optionalView.get(), organizationId, officeId, orgTenant)
                            .then(validateAppointmentLink(tenantId, appointmentId, patientId));
                });
    }

    private Mono<Void> validateCoherence(
            StaffAssignmentReferenceView view,
            UUID encounterOrganizationId,
            UUID encounterOfficeId,
            com.codecore.organization.domain.valueobject.TenantId orgTenant
    ) {
        if (!view.organizationId().value().equals(encounterOrganizationId)) {
            return Mono.error(new EncounterCoherenceException(
                    "Encounter organizationId must equal StaffAssignment organizationId"));
        }

        if (view.officeId().isPresent()) {
            UUID assignmentOfficeId = view.officeId().get().value();
            if (encounterOfficeId == null || !assignmentOfficeId.equals(encounterOfficeId)) {
                return Mono.error(new EncounterCoherenceException(
                        "Encounter officeId must equal StaffAssignment officeId when assignment is office-bound"));
            }
            return Mono.empty();
        }

        // Org-wide assignment: office optional; if present must be ACTIVE in organization.
        if (encounterOfficeId == null) {
            return Mono.empty();
        }

        com.codecore.organization.domain.valueobject.OfficeId officeRef =
                new com.codecore.organization.domain.valueobject.OfficeId(encounterOfficeId);
        return officeReferencePort.existsActiveInOrganization(
                        officeRef,
                        view.organizationId(),
                        orgTenant
                )
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new EncounterReferenceNotFoundException(
                                "Office not found, not ACTIVE, or not in encounter organization"));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> validateAppointmentLink(TenantId tenantId, UUID appointmentId, UUID patientId) {
        if (appointmentId == null) {
            return Mono.empty();
        }

        com.codecore.appointment.domain.valueobject.AppointmentId appointmentRef =
                new com.codecore.appointment.domain.valueobject.AppointmentId(appointmentId);
        com.codecore.appointment.domain.valueobject.TenantId appointmentTenant =
                new com.codecore.appointment.domain.valueobject.TenantId(tenantId.value());

        return appointmentReferencePort.findLinkableByIdAndTenant(appointmentRef, appointmentTenant)
                .flatMap(optionalView -> {
                    if (optionalView.isEmpty()) {
                        return Mono.error(new EncounterReferenceNotFoundException(
                                "Appointment not found, not linkable, or not in tenant"));
                    }
                    if (!optionalView.get().patientId().value().equals(patientId)) {
                        return Mono.error(new EncounterCoherenceException(
                                "Encounter patientId must equal Appointment patientId"));
                    }
                    return Mono.empty();
                });
    }

    private static Instant requireStartedAt(Instant startedAt) {
        if (startedAt == null) {
            throw new InvalidDomainValueException("startedAt is required");
        }
        return startedAt;
    }

    private static Instant resolveEndedAt(Instant bodyEndedAt, Encounter encounter) {
        if (bodyEndedAt != null) {
            return bodyEndedAt;
        }
        return encounter.endedAt().orElseGet(Instant::now);
    }

    private AdminEncounterView toView(Encounter encounter) {
        return new AdminEncounterView(
                encounter.id(),
                encounter.tenantId(),
                encounter.patientId(),
                encounter.staffAssignmentId(),
                encounter.organizationId(),
                encounter.officeId().orElse(null),
                encounter.appointmentId().orElse(null),
                encounter.startedAt(),
                encounter.endedAt().orElse(null),
                encounter.status(),
                encounter.createdAt(),
                encounter.updatedAt()
        );
    }
}
