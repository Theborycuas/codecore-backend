package com.codecore.appointment.domain.model.appointment;

import com.codecore.appointment.domain.exception.InvalidAppointmentStateException;
import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.AppointmentStatus;
import com.codecore.appointment.domain.valueobject.AppointmentTimeWindow;
import com.codecore.appointment.domain.valueobject.OfficeId;
import com.codecore.appointment.domain.valueobject.OrganizationId;
import com.codecore.appointment.domain.valueobject.PatientId;
import com.codecore.appointment.domain.valueobject.StaffAssignmentId;
import com.codecore.appointment.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTest {

    private static final Instant NOW = Instant.parse("2026-07-11T15:00:00Z");
    private static final Instant START = Instant.parse("2026-07-12T14:00:00Z");
    private static final Instant END = Instant.parse("2026-07-12T15:00:00Z");

    @Test
    void shouldScheduleValidAppointment() {
        AppointmentId id = AppointmentId.generate();
        TenantId tenantId = TenantId.generate();
        PatientId patientId = PatientId.of(UUID.randomUUID());
        StaffAssignmentId staffAssignmentId = StaffAssignmentId.of(UUID.randomUUID());
        OrganizationId organizationId = OrganizationId.of(UUID.randomUUID());
        OfficeId officeId = OfficeId.of(UUID.randomUUID());
        AppointmentTimeWindow window = AppointmentTimeWindow.of(START, END);

        Appointment appointment = Appointment.schedule(
                id, tenantId, patientId, staffAssignmentId, organizationId, officeId, window, NOW
        );

        assertThat(appointment.id()).isEqualTo(id);
        assertThat(appointment.tenantId()).isEqualTo(tenantId);
        assertThat(appointment.patientId()).isEqualTo(patientId);
        assertThat(appointment.staffAssignmentId()).isEqualTo(staffAssignmentId);
        assertThat(appointment.organizationId()).isEqualTo(organizationId);
        assertThat(appointment.officeId()).contains(officeId);
        assertThat(appointment.timeWindow()).isEqualTo(window);
        assertThat(appointment.startsAt()).isEqualTo(START);
        assertThat(appointment.endsAt()).isEqualTo(END);
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(appointment.createdAt()).isEqualTo(NOW);
        assertThat(appointment.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldScheduleWithoutOffice() {
        Appointment appointment = scheduledAppointment(null);

        assertThat(appointment.officeId()).isEmpty();
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    void shouldRequireTenantId() {
        assertThatThrownBy(() -> Appointment.schedule(
                AppointmentId.generate(),
                null,
                PatientId.of(UUID.randomUUID()),
                StaffAssignmentId.of(UUID.randomUUID()),
                OrganizationId.of(UUID.randomUUID()),
                null,
                AppointmentTimeWindow.of(START, END),
                NOW
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void shouldKeepTenantIdImmutableAfterMutations() {
        Appointment appointment = scheduledAppointment(OfficeId.of(UUID.randomUUID()));
        TenantId original = appointment.tenantId();

        appointment.reschedule(AppointmentTimeWindow.of(START.plusSeconds(3600), END.plusSeconds(3600)));
        appointment.changePatient(PatientId.of(UUID.randomUUID()));
        appointment.changeStaffAssignment(StaffAssignmentId.of(UUID.randomUUID()));
        appointment.changeOrganization(OrganizationId.of(UUID.randomUUID()));
        appointment.assignOffice(OfficeId.of(UUID.randomUUID()));
        appointment.clearOffice();
        appointment.cancel();

        assertThat(appointment.tenantId()).isEqualTo(original);
    }

    @Test
    void shouldRescheduleWhenScheduled() {
        Appointment appointment = scheduledAppointment(null);
        AppointmentTimeWindow updated = AppointmentTimeWindow.of(
                Instant.parse("2026-07-13T10:00:00Z"),
                Instant.parse("2026-07-13T11:00:00Z")
        );

        appointment.reschedule(updated);

        assertThat(appointment.timeWindow()).isEqualTo(updated);
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(appointment.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldChangeReferencesWhenScheduled() {
        Appointment appointment = scheduledAppointment(null);
        PatientId patientId = PatientId.of(UUID.randomUUID());
        StaffAssignmentId staffAssignmentId = StaffAssignmentId.of(UUID.randomUUID());
        OrganizationId organizationId = OrganizationId.of(UUID.randomUUID());
        OfficeId officeId = OfficeId.of(UUID.randomUUID());

        appointment.changePatient(patientId);
        appointment.changeStaffAssignment(staffAssignmentId);
        appointment.changeOrganization(organizationId);
        appointment.assignOffice(officeId);

        assertThat(appointment.patientId()).isEqualTo(patientId);
        assertThat(appointment.staffAssignmentId()).isEqualTo(staffAssignmentId);
        assertThat(appointment.organizationId()).isEqualTo(organizationId);
        assertThat(appointment.officeId()).contains(officeId);

        appointment.clearOffice();
        assertThat(appointment.officeId()).isEmpty();
        assertThat(appointment.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldCancelFromScheduled() {
        Appointment appointment = scheduledAppointment(null);

        appointment.cancel();

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldCompleteFromScheduled() {
        Appointment appointment = scheduledAppointment(null);

        appointment.complete();

        assertThat(appointment.status()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldRejectMutationsWhenCancelled() {
        Appointment appointment = scheduledAppointment(null);
        appointment.cancel();

        assertThatThrownBy(() -> appointment.reschedule(AppointmentTimeWindow.of(START, END)))
                .isInstanceOf(InvalidAppointmentStateException.class)
                .hasMessageContaining("CANCELLED");

        assertThatThrownBy(() -> appointment.changePatient(PatientId.of(UUID.randomUUID())))
                .isInstanceOf(InvalidAppointmentStateException.class);

        assertThatThrownBy(appointment::cancel)
                .isInstanceOf(InvalidAppointmentStateException.class);

        assertThatThrownBy(appointment::complete)
                .isInstanceOf(InvalidAppointmentStateException.class);
    }

    @Test
    void shouldRejectMutationsWhenCompleted() {
        Appointment appointment = scheduledAppointment(null);
        appointment.complete();

        assertThatThrownBy(() -> appointment.assignOffice(OfficeId.of(UUID.randomUUID())))
                .isInstanceOf(InvalidAppointmentStateException.class)
                .hasMessageContaining("COMPLETED");

        assertThatThrownBy(appointment::clearOffice)
                .isInstanceOf(InvalidAppointmentStateException.class);

        assertThatThrownBy(appointment::cancel)
                .isInstanceOf(InvalidAppointmentStateException.class);

        assertThatThrownBy(appointment::complete)
                .isInstanceOf(InvalidAppointmentStateException.class);
    }

    @Test
    void shouldRejectNullOfficeOnAssign() {
        Appointment appointment = scheduledAppointment(null);

        assertThatThrownBy(() -> appointment.assignOffice(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("newOfficeId");
    }

    @Test
    void shouldReconstituteAppointment() {
        AppointmentId id = AppointmentId.generate();
        TenantId tenantId = TenantId.generate();
        PatientId patientId = PatientId.of(UUID.randomUUID());
        StaffAssignmentId staffAssignmentId = StaffAssignmentId.of(UUID.randomUUID());
        OrganizationId organizationId = OrganizationId.of(UUID.randomUUID());
        OfficeId officeId = OfficeId.of(UUID.randomUUID());
        AppointmentTimeWindow window = AppointmentTimeWindow.of(START, END);
        Instant createdAt = NOW.minusSeconds(3600);
        Instant updatedAt = NOW.minusSeconds(60);

        Appointment appointment = Appointment.reconstitute(
                id,
                tenantId,
                patientId,
                staffAssignmentId,
                organizationId,
                officeId,
                window,
                AppointmentStatus.CANCELLED,
                createdAt,
                updatedAt
        );

        assertThat(appointment.id()).isEqualTo(id);
        assertThat(appointment.tenantId()).isEqualTo(tenantId);
        assertThat(appointment.patientId()).isEqualTo(patientId);
        assertThat(appointment.staffAssignmentId()).isEqualTo(staffAssignmentId);
        assertThat(appointment.organizationId()).isEqualTo(organizationId);
        assertThat(appointment.officeId()).contains(officeId);
        assertThat(appointment.timeWindow()).isEqualTo(window);
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.createdAt()).isEqualTo(createdAt);
        assertThat(appointment.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldNeverExposeClinicalCapacityOrVerticalConcernsInPublicApi() {
        assertThat(Appointment.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain(
                        "addEncounter",
                        "addMedicalRecord",
                        "addNote",
                        "addSoap",
                        "addOdontogram",
                        "addTreatment",
                        "addBillingLine",
                        "addInvoice",
                        "reserveSlot",
                        "addSlot",
                        "setAvailability",
                        "addRecurrence",
                        "addWaitlist",
                        "linkIdentity",
                        "linkMembership"
                );
    }

    private static Appointment scheduledAppointment(OfficeId officeId) {
        return Appointment.schedule(
                AppointmentId.generate(),
                TenantId.generate(),
                PatientId.of(UUID.randomUUID()),
                StaffAssignmentId.of(UUID.randomUUID()),
                OrganizationId.of(UUID.randomUUID()),
                officeId,
                AppointmentTimeWindow.of(START, END),
                NOW
        );
    }
}
