package com.codecore.appointment.infrastructure.adapters;

import com.codecore.appointment.application.port.out.AppointmentRepository;
import com.codecore.appointment.contract.reference.AppointmentReferencePort;
import com.codecore.appointment.contract.reference.AppointmentReferenceView;
import com.codecore.appointment.domain.model.appointment.Appointment;
import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.AppointmentStatus;
import com.codecore.appointment.domain.valueobject.AppointmentTimeWindow;
import com.codecore.appointment.domain.valueobject.OrganizationId;
import com.codecore.appointment.domain.valueobject.PatientId;
import com.codecore.appointment.domain.valueobject.StaffAssignmentId;
import com.codecore.appointment.domain.valueobject.TenantId;
import com.codecore.appointment.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.appointment.testsupport.AppointmentPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({
        AppointmentPersistenceTestConfiguration.class,
        R2dbcAppointmentReferenceAdapter.class
})
class AppointmentReferencePortIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-11T22:00:00Z");
    private static final Instant START = Instant.parse("2026-07-12T14:00:00Z");
    private static final Instant END = Instant.parse("2026-07-12T15:00:00Z");

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentReferencePort appointmentReferencePort;

    @Test
    void shouldReturnTrueForScheduledAppointmentInTenant() {
        AppointmentId appointmentId = AppointmentId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(appointmentRepository.save(scheduled(appointmentId, tenantId, PatientId.of(UUID.randomUUID()))))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentReferencePort.existsScheduledByIdAndTenant(appointmentId, tenantId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseForWrongTenantOrUnknownId() {
        AppointmentId appointmentId = AppointmentId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(appointmentRepository.save(scheduled(appointmentId, tenantId, PatientId.of(UUID.randomUUID()))))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentReferencePort.existsScheduledByIdAndTenant(appointmentId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(appointmentReferencePort.existsScheduledByIdAndTenant(AppointmentId.generate(), tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenAppointmentCancelled() {
        AppointmentId appointmentId = AppointmentId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(appointmentRepository.save(scheduled(appointmentId, tenantId, PatientId.of(UUID.randomUUID())))
                        .flatMap(saved -> {
                            saved.cancel();
                            return appointmentRepository.save(saved);
                        }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentReferencePort.existsScheduledByIdAndTenant(appointmentId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenAppointmentCompletedForExistsScheduled() {
        AppointmentId appointmentId = AppointmentId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(appointmentRepository.save(scheduled(appointmentId, tenantId, PatientId.of(UUID.randomUUID())))
                        .flatMap(saved -> {
                            saved.complete();
                            return appointmentRepository.save(saved);
                        }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentReferencePort.existsScheduledByIdAndTenant(appointmentId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldFindLinkableScheduledWithPatientId() {
        AppointmentId appointmentId = AppointmentId.generate();
        TenantId tenantId = TenantId.generate();
        PatientId patientId = PatientId.of(UUID.randomUUID());

        StepVerifier.create(appointmentRepository.save(scheduled(appointmentId, tenantId, patientId)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentReferencePort.findLinkableByIdAndTenant(appointmentId, tenantId))
                .assertNext(optional -> {
                    assertThat(optional).isPresent();
                    AppointmentReferenceView view = optional.get();
                    assertThat(view.appointmentId()).isEqualTo(appointmentId);
                    assertThat(view.patientId()).isEqualTo(patientId);
                    assertThat(view.status()).isEqualTo(AppointmentStatus.SCHEDULED);
                    assertThat(view.isLinkableForEncounter()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void shouldFindLinkableCompletedWithPatientId() {
        AppointmentId appointmentId = AppointmentId.generate();
        TenantId tenantId = TenantId.generate();
        PatientId patientId = PatientId.of(UUID.randomUUID());

        StepVerifier.create(appointmentRepository.save(scheduled(appointmentId, tenantId, patientId))
                        .flatMap(saved -> {
                            saved.complete();
                            return appointmentRepository.save(saved);
                        }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentReferencePort.findLinkableByIdAndTenant(appointmentId, tenantId))
                .assertNext(optional -> {
                    assertThat(optional).isPresent();
                    assertThat(optional.get().status()).isEqualTo(AppointmentStatus.COMPLETED);
                    assertThat(optional.get().patientId()).isEqualTo(patientId);
                })
                .verifyComplete();

        StepVerifier.create(appointmentReferencePort.existsScheduledByIdAndTenant(appointmentId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyLinkableWhenCancelledOrUnknown() {
        AppointmentId appointmentId = AppointmentId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(appointmentRepository.save(scheduled(appointmentId, tenantId, PatientId.of(UUID.randomUUID())))
                        .flatMap(saved -> {
                            saved.cancel();
                            return appointmentRepository.save(saved);
                        }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentReferencePort.findLinkableByIdAndTenant(appointmentId, tenantId))
                .expectNext(Optional.empty())
                .verifyComplete();

        StepVerifier.create(appointmentReferencePort.findLinkableByIdAndTenant(AppointmentId.generate(), tenantId))
                .expectNext(Optional.empty())
                .verifyComplete();
    }

    private static Appointment scheduled(AppointmentId appointmentId, TenantId tenantId, PatientId patientId) {
        return Appointment.schedule(
                appointmentId,
                tenantId,
                patientId,
                StaffAssignmentId.of(UUID.randomUUID()),
                OrganizationId.of(UUID.randomUUID()),
                null,
                AppointmentTimeWindow.of(START, END),
                NOW
        );
    }
}
