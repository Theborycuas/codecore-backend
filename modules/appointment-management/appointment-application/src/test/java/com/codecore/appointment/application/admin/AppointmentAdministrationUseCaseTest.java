package com.codecore.appointment.application.admin;

import com.codecore.appointment.application.command.CreateAppointmentCommand;
import com.codecore.appointment.application.command.UpdateAppointmentCommand;
import com.codecore.appointment.application.port.out.AppointmentAdminQueryRepository;
import com.codecore.appointment.application.port.out.AppointmentQueryPort;
import com.codecore.appointment.application.port.out.AppointmentRepository;
import com.codecore.appointment.application.port.out.TenantContextAccessor;
import com.codecore.appointment.domain.exception.AppointmentCoherenceException;
import com.codecore.appointment.domain.exception.AppointmentReferenceNotFoundException;
import com.codecore.appointment.domain.exception.InvalidAppointmentStateException;
import com.codecore.appointment.domain.model.appointment.Appointment;
import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.AppointmentStatus;
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
class AppointmentAdministrationUseCaseTest {

    private static final Instant START = Instant.parse("2026-07-12T14:00:00Z");
    private static final Instant END = Instant.parse("2026-07-12T15:00:00Z");

    @Mock
    private TenantContextAccessor tenantContextAccessor;
    @Mock
    private AppointmentAdminQueryRepository appointmentAdminQueryRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentQueryPort appointmentQueryPort;
    @Mock
    private PatientReferencePort patientReferencePort;
    @Mock
    private OrganizationReferencePort organizationReferencePort;
    @Mock
    private OfficeReferencePort officeReferencePort;
    @Mock
    private StaffAssignmentReferencePort staffAssignmentReferencePort;
    @Mock
    private TransactionalOperator transactionalOperator;

    private AppointmentAdministrationUseCaseImpl useCase;
    private TenantId tenantId;
    private UUID patientUuid;
    private UUID orgUuid;
    private UUID staffUuid;
    private UUID officeUuid;

    @BeforeEach
    void setUp() {
        useCase = new AppointmentAdministrationUseCaseImpl(
                tenantContextAccessor,
                appointmentAdminQueryRepository,
                appointmentRepository,
                appointmentQueryPort,
                patientReferencePort,
                organizationReferencePort,
                officeReferencePort,
                staffAssignmentReferencePort,
                transactionalOperator
        );
        tenantId = TenantId.generate();
        patientUuid = UUID.randomUUID();
        orgUuid = UUID.randomUUID();
        staffUuid = UUID.randomUUID();
        officeUuid = UUID.randomUUID();
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateAppointmentWhenRefsValid() {
        stubValidOrgWideRefs();
        when(appointmentRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        CreateAppointmentCommand command = new CreateAppointmentCommand(
                patientUuid, staffUuid, orgUuid, null, START, END
        );

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> {
                    assertThat(view.patientId().value()).isEqualTo(patientUuid);
                    assertThat(view.organizationId().value()).isEqualTo(orgUuid);
                    assertThat(view.officeId()).isNull();
                    assertThat(view.status()).isEqualTo(AppointmentStatus.SCHEDULED);
                    assertThat(view.tenantId()).isEqualTo(tenantId);
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectMissingPatient() {
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.execute(new CreateAppointmentCommand(
                        patientUuid, staffUuid, orgUuid, null, START, END)))
                .expectError(AppointmentReferenceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldRejectMissingOrganization() {
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.execute(new CreateAppointmentCommand(
                        patientUuid, staffUuid, orgUuid, null, START, END)))
                .expectError(AppointmentReferenceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldRejectMissingStaffAssignment() {
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(staffAssignmentReferencePort.findScopeByIdAndTenant(any(), any()))
                .thenReturn(Mono.just(Optional.empty()));

        StepVerifier.create(useCase.execute(new CreateAppointmentCommand(
                        patientUuid, staffUuid, orgUuid, null, START, END)))
                .expectError(AppointmentReferenceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldRejectInvalidOfficeForOrgWideAssignment() {
        stubValidOrgWideRefs();
        when(officeReferencePort.existsActiveInOrganization(any(), any(), any())).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.execute(new CreateAppointmentCommand(
                        patientUuid, staffUuid, orgUuid, officeUuid, START, END)))
                .expectError(AppointmentReferenceNotFoundException.class)
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

        StepVerifier.create(useCase.execute(new CreateAppointmentCommand(
                        patientUuid, staffUuid, orgUuid, null, START, END)))
                .expectError(AppointmentCoherenceException.class)
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

        StepVerifier.create(useCase.execute(new CreateAppointmentCommand(
                        patientUuid, staffUuid, orgUuid, null, START, END)))
                .expectError(AppointmentCoherenceException.class)
                .verify();
    }

    @Test
    void shouldUpdateRescheduleAndRefs() {
        Appointment existing = scheduledAppointment(null);
        stubValidOrgWideRefs();
        when(appointmentQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(existing));
        when(appointmentRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Instant newStart = Instant.parse("2026-07-13T10:00:00Z");
        Instant newEnd = Instant.parse("2026-07-13T11:00:00Z");
        UUID newPatient = UUID.randomUUID();
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.execute(new UpdateAppointmentCommand(
                        existing.id(),
                        newPatient,
                        staffUuid,
                        orgUuid,
                        null,
                        newStart,
                        newEnd
                )))
                .assertNext(view -> {
                    assertThat(view.patientId().value()).isEqualTo(newPatient);
                    assertThat(view.startsAt()).isEqualTo(newStart);
                    assertThat(view.status()).isEqualTo(AppointmentStatus.SCHEDULED);
                })
                .verifyComplete();
    }

    @Test
    void shouldCancelAndComplete() {
        Appointment toCancel = scheduledAppointment(null);
        when(appointmentQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(toCancel));
        when(appointmentRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.cancel(toCancel.id()))
                .assertNext(view -> assertThat(view.status()).isEqualTo(AppointmentStatus.CANCELLED))
                .verifyComplete();

        Appointment toComplete = scheduledAppointment(null);
        when(appointmentQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(toComplete));

        StepVerifier.create(useCase.complete(toComplete.id()))
                .assertNext(view -> assertThat(view.status()).isEqualTo(AppointmentStatus.COMPLETED))
                .verifyComplete();
    }

    @Test
    void shouldRejectCancelWhenAlreadyCancelled() {
        Appointment cancelled = scheduledAppointment(null);
        cancelled.cancel();
        when(appointmentQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(cancelled));

        StepVerifier.create(useCase.cancel(cancelled.id()))
                .expectError(InvalidAppointmentStateException.class)
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
    }

    private Appointment scheduledAppointment(OfficeId officeId) {
        return Appointment.schedule(
                AppointmentId.generate(),
                tenantId,
                PatientId.of(patientUuid),
                StaffAssignmentId.of(staffUuid),
                OrganizationId.of(orgUuid),
                officeId,
                AppointmentTimeWindow.of(START, END),
                Instant.parse("2026-07-11T22:00:00Z")
        );
    }
}
