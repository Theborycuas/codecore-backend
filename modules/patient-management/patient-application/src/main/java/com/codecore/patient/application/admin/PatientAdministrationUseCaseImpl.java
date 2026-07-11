package com.codecore.patient.application.admin;

import com.codecore.organization.contract.reference.OrganizationReferencePort;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.patient.application.command.CreatePatientCommand;
import com.codecore.patient.application.command.UpdatePatientCommand;
import com.codecore.patient.application.dto.AdminPatientView;
import com.codecore.patient.application.dto.PagedResult;
import com.codecore.patient.application.port.in.ActivatePatientUseCase;
import com.codecore.patient.application.port.in.ArchivePatientUseCase;
import com.codecore.patient.application.port.in.CreatePatientUseCase;
import com.codecore.patient.application.port.in.GetPatientUseCase;
import com.codecore.patient.application.port.in.ListPatientsUseCase;
import com.codecore.patient.application.port.in.UpdatePatientUseCase;
import com.codecore.patient.application.port.out.PatientAdminQueryRepository;
import com.codecore.patient.application.port.out.PatientQueryPort;
import com.codecore.patient.application.port.out.PatientRepository;
import com.codecore.patient.application.port.out.TenantContextAccessor;
import com.codecore.patient.application.query.PageQuery;
import com.codecore.patient.application.query.PatientListQuery;
import com.codecore.patient.domain.exception.InvalidDomainValueException;
import com.codecore.patient.domain.exception.PatientNotFoundException;
import com.codecore.patient.domain.exception.PrimaryOrganizationNotFoundException;
import com.codecore.patient.domain.model.patient.Patient;
import com.codecore.patient.domain.valueobject.ContactEmail;
import com.codecore.patient.domain.valueobject.ContactPhone;
import com.codecore.patient.domain.valueobject.DateOfBirth;
import com.codecore.patient.domain.valueobject.ExternalIdentifier;
import com.codecore.patient.domain.valueobject.ExternalIdentifiers;
import com.codecore.patient.domain.valueobject.PatientDemographics;
import com.codecore.patient.domain.valueobject.PatientDisplayName;
import com.codecore.patient.domain.valueobject.PatientId;
import com.codecore.patient.domain.valueobject.PrimaryOrganizationId;
import com.codecore.patient.domain.valueobject.TenantId;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PatientAdministrationUseCaseImpl
        implements ListPatientsUseCase,
        GetPatientUseCase,
        CreatePatientUseCase,
        UpdatePatientUseCase,
        ArchivePatientUseCase,
        ActivatePatientUseCase {

    private final TenantContextAccessor tenantContextAccessor;
    private final PatientAdminQueryRepository patientAdminQueryRepository;
    private final PatientRepository patientRepository;
    private final PatientQueryPort patientQueryPort;
    private final OrganizationReferencePort organizationReferencePort;
    private final TransactionalOperator transactionalOperator;

    public PatientAdministrationUseCaseImpl(
            TenantContextAccessor tenantContextAccessor,
            PatientAdminQueryRepository patientAdminQueryRepository,
            PatientRepository patientRepository,
            PatientQueryPort patientQueryPort,
            OrganizationReferencePort organizationReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        this.tenantContextAccessor = Objects.requireNonNull(tenantContextAccessor, "tenantContextAccessor");
        this.patientAdminQueryRepository = Objects.requireNonNull(
                patientAdminQueryRepository,
                "patientAdminQueryRepository"
        );
        this.patientRepository = Objects.requireNonNull(patientRepository, "patientRepository");
        this.patientQueryPort = Objects.requireNonNull(patientQueryPort, "patientQueryPort");
        this.organizationReferencePort = Objects.requireNonNull(
                organizationReferencePort,
                "organizationReferencePort"
        );
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<PagedResult<AdminPatientView>> execute(PatientListQuery filter, PageQuery pageQuery) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> patientAdminQueryRepository.countByTenantId(tenantId, filter)
                        .flatMap(total -> patientAdminQueryRepository
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
    public Mono<AdminPatientView> execute(PatientId patientId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, patientId).map(this::toView));
    }

    @Override
    public Mono<AdminPatientView> execute(CreatePatientCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> {
                    PatientDemographics demographics = toDemographics(
                            command.displayName(),
                            command.contactEmail(),
                            command.contactPhone(),
                            command.dateOfBirth()
                    );
                    ExternalIdentifiers identifiers = toExternalIdentifiers(command.externalIdentifiers());
                    return resolvePrimaryOrganization(tenantId, command.primaryOrganizationId())
                            .flatMap(primaryOrg -> {
                                Patient patient = Patient.create(
                                        PatientId.generate(),
                                        tenantId,
                                        demographics,
                                        identifiers,
                                        primaryOrg.orElse(null),
                                        Instant.now()
                                );
                                return patientRepository.save(patient).map(this::toView);
                            });
                })
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminPatientView> execute(UpdatePatientCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, command.patientId())
                        .flatMap(patient -> {
                            PatientDemographics demographics = toDemographics(
                                    command.displayName(),
                                    command.contactEmail(),
                                    command.contactPhone(),
                                    command.dateOfBirth()
                            );
                            patient.updateDemographics(demographics);
                            patient.replaceExternalIdentifiers(toExternalIdentifiers(command.externalIdentifiers()));
                            return resolvePrimaryOrganization(tenantId, command.primaryOrganizationId())
                                    .flatMap(primaryOrg -> {
                                        if (primaryOrg.isPresent()) {
                                            patient.assignPrimaryOrganization(primaryOrg.get());
                                        } else {
                                            patient.removePrimaryOrganization();
                                        }
                                        return patientRepository.save(patient);
                                    });
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminPatientView> archive(PatientId patientId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, patientId)
                        .flatMap(patient -> {
                            patient.archive();
                            return patientRepository.save(patient);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminPatientView> activate(PatientId patientId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, patientId)
                        .flatMap(patient -> {
                            patient.activate();
                            return patientRepository.save(patient);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    private Mono<Patient> loadInTenant(TenantId tenantId, PatientId patientId) {
        return patientQueryPort.findByIdAndTenantId(patientId, tenantId)
                .switchIfEmpty(Mono.error(new PatientNotFoundException(
                        "Patient not found in tenant context")));
    }

    private Mono<java.util.Optional<PrimaryOrganizationId>> resolvePrimaryOrganization(
            TenantId tenantId,
            UUID primaryOrganizationId
    ) {
        if (primaryOrganizationId == null) {
            return Mono.just(java.util.Optional.empty());
        }
        OrganizationId organizationId = new OrganizationId(primaryOrganizationId);
        com.codecore.organization.domain.valueobject.TenantId orgTenantId =
                new com.codecore.organization.domain.valueobject.TenantId(tenantId.value());
        return organizationReferencePort.existsActiveByIdAndTenant(organizationId, orgTenantId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new PrimaryOrganizationNotFoundException(
                                "Primary organization not found or not ACTIVE in tenant"));
                    }
                    return Mono.just(java.util.Optional.of(PrimaryOrganizationId.of(primaryOrganizationId)));
                });
    }

    private static PatientDemographics toDemographics(
            String displayName,
            String contactEmail,
            String contactPhone,
            LocalDate dateOfBirth
    ) {
        if (displayName == null || displayName.isBlank()) {
            throw new InvalidDomainValueException("displayName is required");
        }
        return PatientDemographics.of(
                PatientDisplayName.of(displayName),
                blankToNull(contactEmail) == null ? null : ContactEmail.of(contactEmail),
                blankToNull(contactPhone) == null ? null : ContactPhone.of(contactPhone),
                dateOfBirth == null ? null : DateOfBirth.of(dateOfBirth)
        );
    }

    private static ExternalIdentifiers toExternalIdentifiers(
            List<CreatePatientCommand.ExternalIdentifierInput> inputs
    ) {
        if (inputs == null || inputs.isEmpty()) {
            return ExternalIdentifiers.empty();
        }
        List<ExternalIdentifier> identifiers = new ArrayList<>();
        for (CreatePatientCommand.ExternalIdentifierInput input : inputs) {
            identifiers.add(ExternalIdentifier.of(input.type(), input.value()));
        }
        return ExternalIdentifiers.of(identifiers);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private AdminPatientView toView(Patient patient) {
        return new AdminPatientView(
                patient.id(),
                patient.tenantId(),
                patient.demographics().displayName().value(),
                patient.demographics().email().map(ContactEmail::value).orElse(null),
                patient.demographics().phone().map(ContactPhone::value).orElse(null),
                patient.demographics().dateOfBirth().map(DateOfBirth::value).orElse(null),
                patient.primaryOrganizationId().orElse(null),
                patient.externalIdentifiers().asSet().stream()
                        .map(AdminPatientView.ExternalIdentifierItem::from)
                        .toList(),
                patient.status(),
                patient.createdAt(),
                patient.updatedAt()
        );
    }
}
