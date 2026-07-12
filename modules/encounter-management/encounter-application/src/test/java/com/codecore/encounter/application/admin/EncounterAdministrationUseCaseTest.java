package com.codecore.encounter.application.admin;

import com.codecore.appointment.contract.reference.AppointmentReferencePort;
import com.codecore.appointment.contract.reference.AppointmentReferenceView;
import com.codecore.appointment.domain.valueobject.AppointmentStatus;
import com.codecore.encounter.application.command.CreateEncounterCommand;
import com.codecore.encounter.application.command.UpdateEncounterCommand;
import com.codecore.encounter.application.port.out.EncounterAdminQueryRepository;
import com.codecore.encounter.application.port.out.EncounterQueryPort;
import com.codecore.encounter.application.port.out.EncounterRepository;
import com.codecore.encounter.application.port.out.TenantContextAccessor;
import com.codecore.encounter.domain.exception.EncounterCoherenceException;
import com.codecore.encounter.domain.exception.EncounterReferenceNotFoundException;
import com.codecore.encounter.domain.exception.InvalidEncounterStateException;
import com.codecore.encounter.domain.model.encounter.Encounter;
import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.EncounterStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncounterAdministrationUseCaseTest {

    private static final Instant STARTED = Instant.parse("2026-07-12T14:00:00Z");
    private static final Instant ENDED = Instant.parse("2026-07-12T15:00:00Z");

    @Mock
    private TenantContextAccessor tenantContextAccessor;
    @Mock
    private EncounterAdminQueryRepository encounterAdminQueryRepository;
    @Mock
    private EncounterRepository encounterRepository;
    @Mock
    private EncounterQueryPort encounterQueryPort;
    @Mock
    private PatientReferencePort patientReferencePort;
    @Mock
    private OrganizationReferencePort organizationReferencePort;
    @Mock
    private OfficeReferencePort officeReferencePort;
    @Mock
    private StaffAssignmentReferencePort staffAssignmentReferencePort;
    @Mock
    private AppointmentReferencePort appointmentReferencePort;
    @Mock
    private TransactionalOperator transactionalOperator;

    private EncounterAdministrationUseCaseImpl useCase;
    private TenantId tenantId;
    private UUID patientUuid;
    private UUID orgUuid;
    private UUID staffUuid;
    private UUID officeUuid;
    private UUID appointmentUuid;

    @BeforeEach
    void setUp() {
        useCase = new EncounterAdministrationUseCaseImpl(
                tenantContextAccessor,
                encounterAdminQueryRepository,
                encounterRepository,
                encounterQueryPort,
                patientReferencePort,
                organizationReferencePort,
                officeReferencePort,
                staffAssignmentReferencePort,
                appointmentReferencePort,
                transactionalOperator
        );
        tenantId = TenantId.generate();
        patientUuid = UUID.randomUUID();
        orgUuid = UUID.randomUUID();
        staffUuid = UUID.randomUUID();
        officeUuid = UUID.randomUUID();
        appointmentUuid = UUID.randomUUID();
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateEncounterWhenRefsValid() {
        stubValidOrgWideRefs();
        when(encounterRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        CreateEncounterCommand command = new CreateEncounterCommand(
                patientUuid, staffUuid, orgUuid, null, null, STARTED, null
        );

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> {
                    assertThat(view.patientId().value()).isEqualTo(patientUuid);
                    assertThat(view.organizationId().value()).isEqualTo(orgUuid);
                    assertThat(view.officeId()).isNull();
                    assertThat(view.appointmentId()).isNull();
                    assertThat(view.status()).isEqualTo(EncounterStatus.IN_PROGRESS);
                    assertThat(view.endedAt()).isNull();
                    assertThat(view.tenantId()).isEqualTo(tenantId);
                })
                .verifyComplete();
    }

    @Test
    void shouldCreateWithEndedAtStillInProgress() {
        stubValidOrgWideRefs();
        when(encounterRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute(new CreateEncounterCommand(
                        patientUuid, staffUuid, orgUuid, null, null, STARTED, ENDED)))
                .assertNext(view -> {
                    assertThat(view.status()).isEqualTo(EncounterStatus.IN_PROGRESS);
                    assertThat(view.endedAt()).isEqualTo(ENDED);
                })
                .verifyComplete();
    }

    @Test
    void shouldCreateWithLinkableAppointment() {
        stubValidOrgWideRefs();
        stubLinkableAppointment(patientUuid);
        when(encounterRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute(new CreateEncounterCommand(
                        patientUuid, staffUuid, orgUuid, null, appointmentUuid, STARTED, null)))
                .assertNext(view -> {
                    assertThat(view.appointmentId().value()).isEqualTo(appointmentUuid);
                    assertThat(view.status()).isEqualTo(EncounterStatus.IN_PROGRESS);
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectAppointmentPatientMismatch() {
        stubValidOrgWideRefs();
        stubLinkableAppointment(UUID.randomUUID());

        StepVerifier.create(useCase.execute(new CreateEncounterCommand(
                        patientUuid, staffUuid, orgUuid, null, appointmentUuid, STARTED, null)))
                .expectError(EncounterCoherenceException.class)
                .verify();
    }

    @Test
    void shouldRejectMissingAppointment() {
        stubValidOrgWideRefs();
        when(appointmentReferencePort.findLinkableByIdAndTenant(any(), any()))
                .thenReturn(Mono.just(Optional.empty()));

        StepVerifier.create(useCase.execute(new CreateEncounterCommand(
                        patientUuid, staffUuid, orgUuid, null, appointmentUuid, STARTED, null)))
                .expectError(EncounterReferenceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldRejectMissingPatient() {
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.execute(new CreateEncounterCommand(
                        patientUuid, staffUuid, orgUuid, null, null, STARTED, null)))
                .expectError(EncounterReferenceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldRejectMissingOrganization() {
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.execute(new CreateEncounterCommand(
                        patientUuid, staffUuid, orgUuid, null, null, STARTED, null)))
                .expectError(EncounterReferenceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldRejectMissingStaffAssignment() {
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(staffAssignmentReferencePort.findScopeByIdAndTenant(any(), any()))
                .thenReturn(Mono.just(Optional.empty()));

        StepVerifier.create(useCase.execute(new CreateEncounterCommand(
                        patientUuid, staffUuid, orgUuid, null, null, STARTED, null)))
                .expectError(EncounterReferenceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldRejectInvalidOfficeForOrgWideAssignment() {
        stubValidOrgWideRefs();
        when(officeReferencePort.existsActiveInOrganization(any(), any(), any())).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.execute(new CreateEncounterCommand(
                        patientUuid, staffUuid, orgUuid, officeUuid, null, STARTED, null)))
                .expectError(EncounterReferenceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldRejectStaffOfficeCoherenceMismatch() {
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(staffAssignmentReferencePort.findScopeByIdAndTenant(any(), any()))
                .thenReturn(Mono.just(Optional.of(new StaffAssignmentReferenceView(
                        new com.codecore.organization.domain.valueobject.StaffAssignmentId(staffUuid),
                        new com.codecore.organization.domain.valueobject.OrganizationId(orgUuid),
                        new com.codecore.organization.domain.valueobject.OfficeId(officeUuid)
                ))));

        StepVerifier.create(useCase.execute(new CreateEncounterCommand(
                        patientUuid, staffUuid, orgUuid, null, null, STARTED, null)))
                .expectError(EncounterCoherenceException.class)
                .verify();
    }

    @Test
    void shouldRejectOrganizationCoherenceMismatch() {
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(staffAssignmentReferencePort.findScopeByIdAndTenant(any(), any()))
                .thenReturn(Mono.just(Optional.of(new StaffAssignmentReferenceView(
                        new com.codecore.organization.domain.valueobject.StaffAssignmentId(staffUuid),
                        new com.codecore.organization.domain.valueobject.OrganizationId(UUID.randomUUID()),
                        null
                ))));

        StepVerifier.create(useCase.execute(new CreateEncounterCommand(
                        patientUuid, staffUuid, orgUuid, null, null, STARTED, null)))
                .expectError(EncounterCoherenceException.class)
                .verify();
    }

    @Test
    void shouldUpdateRefsAndTimeBounds() {
        Encounter existing = openEncounter(null);
        stubValidOrgWideRefs();
        when(encounterQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(existing));
        when(encounterRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Instant newStarted = Instant.parse("2026-07-13T10:00:00Z");
        UUID newPatient = UUID.randomUUID();
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.execute(new UpdateEncounterCommand(
                        existing.id(),
                        newPatient,
                        staffUuid,
                        orgUuid,
                        null,
                        null,
                        newStarted,
                        null
                )))
                .assertNext(view -> {
                    assertThat(view.patientId().value()).isEqualTo(newPatient);
                    assertThat(view.startedAt()).isEqualTo(newStarted);
                    assertThat(view.status()).isEqualTo(EncounterStatus.IN_PROGRESS);
                })
                .verifyComplete();
    }

    @Test
    void shouldCancelAndComplete() {
        Encounter toCancel = openEncounter(null);
        when(encounterQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(toCancel));
        when(encounterRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.cancel(toCancel.id()))
                .assertNext(view -> assertThat(view.status()).isEqualTo(EncounterStatus.CANCELLED))
                .verifyComplete();

        Encounter toComplete = openEncounter(null);
        when(encounterQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(toComplete));

        StepVerifier.create(useCase.complete(toComplete.id(), ENDED))
                .assertNext(view -> {
                    assertThat(view.status()).isEqualTo(EncounterStatus.COMPLETED);
                    assertThat(view.endedAt()).isEqualTo(ENDED);
                })
                .verifyComplete();
    }

    @Test
    void shouldCompleteUsingPersistedEndedAtWhenBodyOmitted() {
        Encounter existing = openEncounter(null);
        existing.assignEndedAt(ENDED);
        when(encounterQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(existing));
        when(encounterRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.complete(existing.id(), null))
                .assertNext(view -> {
                    assertThat(view.status()).isEqualTo(EncounterStatus.COMPLETED);
                    assertThat(view.endedAt()).isEqualTo(ENDED);
                })
                .verifyComplete();
    }

    @Test
    void shouldCompleteUsingNowWhenNoEndedAtAnywhere() {
        Instant pastStarted = Instant.parse("2020-01-01T10:00:00Z");
        Encounter existing = Encounter.open(
                EncounterId.generate(),
                tenantId,
                PatientId.of(patientUuid),
                StaffAssignmentId.of(staffUuid),
                OrganizationId.of(orgUuid),
                null,
                null,
                pastStarted,
                Instant.parse("2020-01-01T09:00:00Z")
        );
        Instant before = Instant.now().minusSeconds(1);
        when(encounterQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(existing));
        when(encounterRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.complete(existing.id(), null))
                .assertNext(view -> {
                    assertThat(view.status()).isEqualTo(EncounterStatus.COMPLETED);
                    assertThat(view.endedAt()).isNotNull();
                    assertThat(view.endedAt()).isAfterOrEqualTo(before);
                    assertThat(view.endedAt()).isAfterOrEqualTo(pastStarted);
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectCancelWhenAlreadyCancelled() {
        Encounter cancelled = openEncounter(null);
        cancelled.cancel();
        when(encounterQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(cancelled));

        StepVerifier.create(useCase.cancel(cancelled.id()))
                .expectError(InvalidEncounterStateException.class)
                .verify();
    }

    private void stubValidOrgWideRefs() {
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(staffAssignmentReferencePort.findScopeByIdAndTenant(any(), any()))
                .thenReturn(Mono.just(Optional.of(new StaffAssignmentReferenceView(
                        new com.codecore.organization.domain.valueobject.StaffAssignmentId(staffUuid),
                        new com.codecore.organization.domain.valueobject.OrganizationId(orgUuid),
                        null
                ))));
        lenient().when(officeReferencePort.existsActiveInOrganization(any(), any(), any()))
                .thenReturn(Mono.just(true));
        lenient().when(appointmentReferencePort.findLinkableByIdAndTenant(any(), any()))
                .thenReturn(Mono.just(Optional.empty()));
    }

    private void stubLinkableAppointment(UUID appointmentPatientId) {
        when(appointmentReferencePort.findLinkableByIdAndTenant(any(), any()))
                .thenReturn(Mono.just(Optional.of(new AppointmentReferenceView(
                        new com.codecore.appointment.domain.valueobject.AppointmentId(appointmentUuid),
                        com.codecore.appointment.domain.valueobject.PatientId.of(appointmentPatientId),
                        AppointmentStatus.SCHEDULED
                ))));
    }

    private Encounter openEncounter(OfficeId officeId) {
        return Encounter.open(
                EncounterId.generate(),
                tenantId,
                PatientId.of(patientUuid),
                StaffAssignmentId.of(staffUuid),
                OrganizationId.of(orgUuid),
                officeId,
                null,
                STARTED,
                Instant.parse("2026-07-11T22:00:00Z")
        );
    }
}
