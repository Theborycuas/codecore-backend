package com.codecore.encounter.domain.model.encounter;

import com.codecore.encounter.domain.exception.InvalidEncounterStateException;
import com.codecore.encounter.domain.valueobject.AppointmentId;
import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.EncounterStatus;
import com.codecore.encounter.domain.valueobject.EncounterTimeBounds;
import com.codecore.encounter.domain.valueobject.OfficeId;
import com.codecore.encounter.domain.valueobject.OrganizationId;
import com.codecore.encounter.domain.valueobject.PatientId;
import com.codecore.encounter.domain.valueobject.StaffAssignmentId;
import com.codecore.encounter.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncounterTest {

    private static final Instant NOW = Instant.parse("2026-07-11T15:00:00Z");
    private static final Instant STARTED = Instant.parse("2026-07-12T14:00:00Z");
    private static final Instant ENDED = Instant.parse("2026-07-12T15:00:00Z");

    @Test
    void shouldOpenValidEncounter() {
        EncounterId id = EncounterId.generate();
        TenantId tenantId = TenantId.generate();
        PatientId patientId = PatientId.of(UUID.randomUUID());
        StaffAssignmentId staffAssignmentId = StaffAssignmentId.of(UUID.randomUUID());
        OrganizationId organizationId = OrganizationId.of(UUID.randomUUID());
        OfficeId officeId = OfficeId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());

        Encounter encounter = Encounter.open(
                id, tenantId, patientId, staffAssignmentId, organizationId,
                officeId, appointmentId, STARTED, NOW
        );

        assertThat(encounter.id()).isEqualTo(id);
        assertThat(encounter.tenantId()).isEqualTo(tenantId);
        assertThat(encounter.patientId()).isEqualTo(patientId);
        assertThat(encounter.staffAssignmentId()).isEqualTo(staffAssignmentId);
        assertThat(encounter.organizationId()).isEqualTo(organizationId);
        assertThat(encounter.officeId()).contains(officeId);
        assertThat(encounter.appointmentId()).contains(appointmentId);
        assertThat(encounter.startedAt()).isEqualTo(STARTED);
        assertThat(encounter.endedAt()).isEmpty();
        assertThat(encounter.status()).isEqualTo(EncounterStatus.IN_PROGRESS);
        assertThat(encounter.createdAt()).isEqualTo(NOW);
        assertThat(encounter.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldOpenWithoutOfficeOrAppointment() {
        Encounter encounter = openEncounter(null, null);

        assertThat(encounter.officeId()).isEmpty();
        assertThat(encounter.appointmentId()).isEmpty();
        assertThat(encounter.status()).isEqualTo(EncounterStatus.IN_PROGRESS);
    }

    @Test
    void shouldRequireTenantId() {
        assertThatThrownBy(() -> Encounter.open(
                EncounterId.generate(),
                null,
                PatientId.of(UUID.randomUUID()),
                StaffAssignmentId.of(UUID.randomUUID()),
                OrganizationId.of(UUID.randomUUID()),
                null,
                null,
                STARTED,
                NOW
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void shouldKeepTenantIdImmutableAfterMutations() {
        Encounter encounter = openEncounter(OfficeId.of(UUID.randomUUID()), AppointmentId.of(UUID.randomUUID()));
        TenantId original = encounter.tenantId();

        encounter.changeStartedAt(STARTED.plusSeconds(60));
        encounter.assignEndedAt(ENDED);
        encounter.clearEndedAt();
        encounter.changePatient(PatientId.of(UUID.randomUUID()));
        encounter.changeStaffAssignment(StaffAssignmentId.of(UUID.randomUUID()));
        encounter.changeOrganization(OrganizationId.of(UUID.randomUUID()));
        encounter.assignOffice(OfficeId.of(UUID.randomUUID()));
        encounter.clearOffice();
        encounter.linkAppointment(AppointmentId.of(UUID.randomUUID()));
        encounter.clearAppointment();
        encounter.cancel();

        assertThat(encounter.tenantId()).isEqualTo(original);
    }

    @Test
    void shouldAdjustTimeBoundsWhenInProgress() {
        Encounter encounter = openEncounter(null, null);

        encounter.changeStartedAt(STARTED.plusSeconds(120));
        encounter.assignEndedAt(ENDED);

        assertThat(encounter.startedAt()).isEqualTo(STARTED.plusSeconds(120));
        assertThat(encounter.endedAt()).contains(ENDED);
        assertThat(encounter.status()).isEqualTo(EncounterStatus.IN_PROGRESS);

        encounter.clearEndedAt();
        assertThat(encounter.endedAt()).isEmpty();
        assertThat(encounter.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldChangeReferencesWhenInProgress() {
        Encounter encounter = openEncounter(null, null);
        PatientId patientId = PatientId.of(UUID.randomUUID());
        StaffAssignmentId staffAssignmentId = StaffAssignmentId.of(UUID.randomUUID());
        OrganizationId organizationId = OrganizationId.of(UUID.randomUUID());
        OfficeId officeId = OfficeId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());

        encounter.changePatient(patientId);
        encounter.changeStaffAssignment(staffAssignmentId);
        encounter.changeOrganization(organizationId);
        encounter.assignOffice(officeId);
        encounter.linkAppointment(appointmentId);

        assertThat(encounter.patientId()).isEqualTo(patientId);
        assertThat(encounter.staffAssignmentId()).isEqualTo(staffAssignmentId);
        assertThat(encounter.organizationId()).isEqualTo(organizationId);
        assertThat(encounter.officeId()).contains(officeId);
        assertThat(encounter.appointmentId()).contains(appointmentId);

        encounter.clearOffice();
        encounter.clearAppointment();
        assertThat(encounter.officeId()).isEmpty();
        assertThat(encounter.appointmentId()).isEmpty();
    }

    @Test
    void shouldCancelFromInProgress() {
        Encounter encounter = openEncounter(null, null);

        encounter.cancel();

        assertThat(encounter.status()).isEqualTo(EncounterStatus.CANCELLED);
        assertThat(encounter.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldCompleteFromInProgressWithEndedAt() {
        Encounter encounter = openEncounter(null, null);

        encounter.complete(ENDED);

        assertThat(encounter.status()).isEqualTo(EncounterStatus.COMPLETED);
        assertThat(encounter.endedAt()).contains(ENDED);
        assertThat(encounter.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldAllowCompleteWithEndedAtEqualToStartedAt() {
        Encounter encounter = openEncounter(null, null);

        encounter.complete(STARTED);

        assertThat(encounter.status()).isEqualTo(EncounterStatus.COMPLETED);
        assertThat(encounter.endedAt()).contains(STARTED);
    }

    @Test
    void shouldRejectMutationsWhenCancelled() {
        Encounter encounter = openEncounter(null, null);
        encounter.cancel();

        assertThatThrownBy(() -> encounter.changeStartedAt(STARTED))
                .isInstanceOf(InvalidEncounterStateException.class)
                .hasMessageContaining("CANCELLED");

        assertThatThrownBy(() -> encounter.linkAppointment(AppointmentId.of(UUID.randomUUID())))
                .isInstanceOf(InvalidEncounterStateException.class);

        assertThatThrownBy(encounter::cancel)
                .isInstanceOf(InvalidEncounterStateException.class);

        assertThatThrownBy(() -> encounter.complete(ENDED))
                .isInstanceOf(InvalidEncounterStateException.class);
    }

    @Test
    void shouldRejectMutationsWhenCompleted() {
        Encounter encounter = openEncounter(null, null);
        encounter.complete(ENDED);

        assertThatThrownBy(() -> encounter.assignOffice(OfficeId.of(UUID.randomUUID())))
                .isInstanceOf(InvalidEncounterStateException.class)
                .hasMessageContaining("COMPLETED");

        assertThatThrownBy(encounter::clearAppointment)
                .isInstanceOf(InvalidEncounterStateException.class);

        assertThatThrownBy(encounter::cancel)
                .isInstanceOf(InvalidEncounterStateException.class);

        assertThatThrownBy(() -> encounter.complete(ENDED))
                .isInstanceOf(InvalidEncounterStateException.class);
    }

    @Test
    void shouldRejectNullEndedAtOnComplete() {
        Encounter encounter = openEncounter(null, null);

        assertThatThrownBy(() -> encounter.complete(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("newEndedAt");
    }

    @Test
    void shouldReconstituteEncounter() {
        EncounterId id = EncounterId.generate();
        TenantId tenantId = TenantId.generate();
        PatientId patientId = PatientId.of(UUID.randomUUID());
        StaffAssignmentId staffAssignmentId = StaffAssignmentId.of(UUID.randomUUID());
        OrganizationId organizationId = OrganizationId.of(UUID.randomUUID());
        OfficeId officeId = OfficeId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());
        EncounterTimeBounds bounds = EncounterTimeBounds.of(STARTED, ENDED);
        Instant createdAt = NOW.minusSeconds(3600);
        Instant updatedAt = NOW.minusSeconds(60);

        Encounter encounter = Encounter.reconstitute(
                id,
                tenantId,
                patientId,
                staffAssignmentId,
                organizationId,
                officeId,
                appointmentId,
                bounds,
                EncounterStatus.COMPLETED,
                createdAt,
                updatedAt
        );

        assertThat(encounter.id()).isEqualTo(id);
        assertThat(encounter.tenantId()).isEqualTo(tenantId);
        assertThat(encounter.patientId()).isEqualTo(patientId);
        assertThat(encounter.staffAssignmentId()).isEqualTo(staffAssignmentId);
        assertThat(encounter.organizationId()).isEqualTo(organizationId);
        assertThat(encounter.officeId()).contains(officeId);
        assertThat(encounter.appointmentId()).contains(appointmentId);
        assertThat(encounter.timeBounds()).isEqualTo(bounds);
        assertThat(encounter.status()).isEqualTo(EncounterStatus.COMPLETED);
        assertThat(encounter.createdAt()).isEqualTo(createdAt);
        assertThat(encounter.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldNeverExposeClinicalDocumentationOrVerticalConcernsInPublicApi() {
        assertThat(Encounter.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain(
                        "addNote",
                        "addSoap",
                        "addMedicalRecord",
                        "addOdontogram",
                        "addDiagnosis",
                        "addPrescription",
                        "addTreatment",
                        "addObservation",
                        "addLabResult",
                        "addAttachment",
                        "addClinicalImage",
                        "addBillingLine",
                        "addInvoice",
                        "linkIdentity",
                        "linkMembership",
                        "completeAppointment",
                        "createFromAppointment"
                );
    }

    private static Encounter openEncounter(OfficeId officeId, AppointmentId appointmentId) {
        return Encounter.open(
                EncounterId.generate(),
                TenantId.generate(),
                PatientId.of(UUID.randomUUID()),
                StaffAssignmentId.of(UUID.randomUUID()),
                OrganizationId.of(UUID.randomUUID()),
                officeId,
                appointmentId,
                STARTED,
                NOW
        );
    }
}
