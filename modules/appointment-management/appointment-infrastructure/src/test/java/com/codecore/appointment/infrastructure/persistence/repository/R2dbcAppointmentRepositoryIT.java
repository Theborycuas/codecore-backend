package com.codecore.appointment.infrastructure.persistence.repository;

import com.codecore.appointment.application.port.out.AppointmentQueryPort;
import com.codecore.appointment.application.port.out.AppointmentRepository;
import com.codecore.appointment.domain.model.appointment.Appointment;
import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.AppointmentStatus;
import com.codecore.appointment.domain.valueobject.AppointmentTimeWindow;
import com.codecore.appointment.domain.valueobject.OfficeId;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(AppointmentPersistenceTestConfiguration.class)
class R2dbcAppointmentRepositoryIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-11T22:00:00Z");
    private static final Instant START = Instant.parse("2026-07-12T14:00:00Z");
    private static final Instant END = Instant.parse("2026-07-12T15:00:00Z");

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentQueryPort appointmentQueryPort;

    @Test
    void shouldPersistAndFindById() {
        AppointmentId appointmentId = AppointmentId.generate();
        TenantId tenantId = TenantId.generate();
        PatientId patientId = PatientId.of(UUID.randomUUID());
        StaffAssignmentId staffAssignmentId = StaffAssignmentId.of(UUID.randomUUID());
        OrganizationId organizationId = OrganizationId.of(UUID.randomUUID());
        OfficeId officeId = OfficeId.of(UUID.randomUUID());

        Appointment appointment = Appointment.schedule(
                appointmentId,
                tenantId,
                patientId,
                staffAssignmentId,
                organizationId,
                officeId,
                AppointmentTimeWindow.of(START, END),
                NOW
        );

        StepVerifier.create(appointmentRepository.save(appointment))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(appointmentId);
                    assertThat(saved.tenantId()).isEqualTo(tenantId);
                    assertThat(saved.patientId()).isEqualTo(patientId);
                    assertThat(saved.staffAssignmentId()).isEqualTo(staffAssignmentId);
                    assertThat(saved.organizationId()).isEqualTo(organizationId);
                    assertThat(saved.officeId()).contains(officeId);
                    assertThat(saved.startsAt()).isEqualTo(START);
                    assertThat(saved.endsAt()).isEqualTo(END);
                    assertThat(saved.status()).isEqualTo(AppointmentStatus.SCHEDULED);
                })
                .verifyComplete();

        StepVerifier.create(appointmentRepository.findById(appointmentId))
                .assertNext(found -> {
                    assertThat(found.id()).isEqualTo(appointmentId);
                    assertThat(found.officeId()).contains(officeId);
                    assertThat(found.status()).isEqualTo(AppointmentStatus.SCHEDULED);
                })
                .verifyComplete();
    }

    @Test
    void shouldReportExistsByIdAndTenant() {
        AppointmentId appointmentId = AppointmentId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(appointmentRepository.save(scheduledAppointment(appointmentId, tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentRepository.existsById(appointmentId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(appointmentRepository.existsByIdAndTenantId(appointmentId, tenantId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(appointmentRepository.existsByIdAndTenantId(appointmentId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(appointmentRepository.existsById(AppointmentId.generate()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldCountAndFindByTenantId() {
        TenantId tenantId = TenantId.generate();
        TenantId otherTenantId = TenantId.generate();

        StepVerifier.create(appointmentRepository.save(scheduledAppointment(AppointmentId.generate(), tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(appointmentRepository.save(scheduledAppointment(AppointmentId.generate(), tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(appointmentRepository.save(scheduledAppointment(AppointmentId.generate(), otherTenantId, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentQueryPort.countByTenantId(tenantId))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(appointmentQueryPort.findByTenantId(tenantId))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldIsolateCrossTenantReads() {
        AppointmentId appointmentId = AppointmentId.generate();
        TenantId tenantId = TenantId.generate();
        TenantId otherTenantId = TenantId.generate();

        StepVerifier.create(appointmentRepository.save(scheduledAppointment(appointmentId, tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentQueryPort.findByIdAndTenantId(appointmentId, tenantId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(appointmentId))
                .verifyComplete();

        StepVerifier.create(appointmentQueryPort.findByIdAndTenantId(appointmentId, otherTenantId))
                .verifyComplete();
    }

    @Test
    void shouldPersistNullAndNonNullOfficeId() {
        TenantId tenantId = TenantId.generate();
        OfficeId officeId = OfficeId.of(UUID.randomUUID());
        AppointmentId withoutOffice = AppointmentId.generate();
        AppointmentId withOffice = AppointmentId.generate();

        StepVerifier.create(appointmentRepository.save(scheduledAppointment(withoutOffice, tenantId, null)))
                .assertNext(saved -> assertThat(saved.officeId()).isEmpty())
                .verifyComplete();

        StepVerifier.create(appointmentRepository.save(scheduledAppointment(withOffice, tenantId, officeId)))
                .assertNext(saved -> assertThat(saved.officeId()).contains(officeId))
                .verifyComplete();
    }

    @Test
    void shouldFindByTenantIdAndOrganizationId() {
        TenantId tenantId = TenantId.generate();
        OrganizationId organizationId = OrganizationId.of(UUID.randomUUID());
        AppointmentId matchingId = AppointmentId.generate();

        Appointment matching = Appointment.schedule(
                matchingId,
                tenantId,
                PatientId.of(UUID.randomUUID()),
                StaffAssignmentId.of(UUID.randomUUID()),
                organizationId,
                null,
                AppointmentTimeWindow.of(START, END),
                NOW
        );
        Appointment other = scheduledAppointment(AppointmentId.generate(), tenantId, null);

        StepVerifier.create(appointmentRepository.save(matching))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(appointmentRepository.save(other))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentQueryPort.findByTenantIdAndOrganizationId(tenantId, organizationId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(matchingId))
                .verifyComplete();
    }

    @Test
    void shouldFindByTenantIdAndPatientId() {
        TenantId tenantId = TenantId.generate();
        PatientId patientId = PatientId.of(UUID.randomUUID());
        AppointmentId matchingId = AppointmentId.generate();

        Appointment matching = Appointment.schedule(
                matchingId,
                tenantId,
                patientId,
                StaffAssignmentId.of(UUID.randomUUID()),
                OrganizationId.of(UUID.randomUUID()),
                null,
                AppointmentTimeWindow.of(START, END),
                NOW
        );

        StepVerifier.create(appointmentRepository.save(matching))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(appointmentRepository.save(scheduledAppointment(AppointmentId.generate(), tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentQueryPort.findByTenantIdAndPatientId(tenantId, patientId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(matchingId))
                .verifyComplete();
    }

    @Test
    void shouldFindByTenantIdAndStaffAssignmentId() {
        TenantId tenantId = TenantId.generate();
        StaffAssignmentId staffAssignmentId = StaffAssignmentId.of(UUID.randomUUID());
        AppointmentId matchingId = AppointmentId.generate();

        Appointment matching = Appointment.schedule(
                matchingId,
                tenantId,
                PatientId.of(UUID.randomUUID()),
                staffAssignmentId,
                OrganizationId.of(UUID.randomUUID()),
                null,
                AppointmentTimeWindow.of(START, END),
                NOW
        );

        StepVerifier.create(appointmentRepository.save(matching))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(appointmentRepository.save(scheduledAppointment(AppointmentId.generate(), tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentQueryPort.findByTenantIdAndStaffAssignmentId(tenantId, staffAssignmentId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(matchingId))
                .verifyComplete();
    }

    @Test
    void shouldFindByTenantIdAndStatus() {
        TenantId tenantId = TenantId.generate();
        AppointmentId scheduledId = AppointmentId.generate();
        AppointmentId cancelledId = AppointmentId.generate();

        StepVerifier.create(appointmentRepository.save(scheduledAppointment(scheduledId, tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();

        Appointment cancelled = scheduledAppointment(cancelledId, tenantId, null);
        cancelled.cancel();

        StepVerifier.create(appointmentRepository.save(cancelled))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(appointmentQueryPort.findByTenantIdAndStatus(tenantId, AppointmentStatus.SCHEDULED))
                .assertNext(found -> assertThat(found.id()).isEqualTo(scheduledId))
                .verifyComplete();

        StepVerifier.create(appointmentQueryPort.findByTenantIdAndStatus(tenantId, AppointmentStatus.CANCELLED))
                .assertNext(found -> assertThat(found.id()).isEqualTo(cancelledId))
                .verifyComplete();
    }

    @Test
    void shouldUpdateExistingAppointmentWithoutDuplicatingRow() {
        AppointmentId appointmentId = AppointmentId.generate();
        TenantId tenantId = TenantId.generate();
        PatientId patientId = PatientId.of(UUID.randomUUID());
        StaffAssignmentId staffAssignmentId = StaffAssignmentId.of(UUID.randomUUID());
        OrganizationId organizationId = OrganizationId.of(UUID.randomUUID());

        StepVerifier.create(appointmentRepository.save(Appointment.schedule(
                appointmentId,
                tenantId,
                patientId,
                staffAssignmentId,
                organizationId,
                null,
                AppointmentTimeWindow.of(START, END),
                NOW
        )))
                .expectNextCount(1)
                .verifyComplete();

        Instant newStart = Instant.parse("2026-07-13T10:00:00Z");
        Instant newEnd = Instant.parse("2026-07-13T11:00:00Z");
        Appointment rescheduled = Appointment.reconstitute(
                appointmentId,
                tenantId,
                patientId,
                staffAssignmentId,
                organizationId,
                OfficeId.of(UUID.randomUUID()),
                AppointmentTimeWindow.of(newStart, newEnd),
                AppointmentStatus.SCHEDULED,
                NOW,
                NOW.plusSeconds(60)
        );

        StepVerifier.create(appointmentRepository.save(rescheduled))
                .assertNext(saved -> {
                    assertThat(saved.startsAt()).isEqualTo(newStart);
                    assertThat(saved.endsAt()).isEqualTo(newEnd);
                    assertThat(saved.officeId()).isPresent();
                })
                .verifyComplete();

        StepVerifier.create(appointmentQueryPort.countByTenantId(tenantId))
                .expectNext(1L)
                .verifyComplete();
    }

    private static Appointment scheduledAppointment(
            AppointmentId appointmentId,
            TenantId tenantId,
            OfficeId officeId
    ) {
        return Appointment.schedule(
                appointmentId,
                tenantId,
                PatientId.of(UUID.randomUUID()),
                StaffAssignmentId.of(UUID.randomUUID()),
                OrganizationId.of(UUID.randomUUID()),
                officeId,
                AppointmentTimeWindow.of(START, END),
                NOW
        );
    }
}
