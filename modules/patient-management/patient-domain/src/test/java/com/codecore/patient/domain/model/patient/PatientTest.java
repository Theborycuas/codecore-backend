package com.codecore.patient.domain.model.patient;

import com.codecore.patient.domain.exception.InvalidPatientStateException;
import com.codecore.patient.domain.valueobject.ContactEmail;
import com.codecore.patient.domain.valueobject.ContactPhone;
import com.codecore.patient.domain.valueobject.DateOfBirth;
import com.codecore.patient.domain.valueobject.ExternalIdentifier;
import com.codecore.patient.domain.valueobject.ExternalIdentifiers;
import com.codecore.patient.domain.valueobject.PatientDemographics;
import com.codecore.patient.domain.valueobject.PatientDisplayName;
import com.codecore.patient.domain.valueobject.PatientId;
import com.codecore.patient.domain.valueobject.PatientStatus;
import com.codecore.patient.domain.valueobject.PrimaryOrganizationId;
import com.codecore.patient.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientTest {

    private static final Instant NOW = Instant.parse("2026-07-11T15:00:00Z");

    @Test
    void shouldCreateValidPatient() {
        PatientId id = PatientId.generate();
        TenantId tenantId = TenantId.generate();
        PatientDemographics demographics = PatientDemographics.of(PatientDisplayName.of("María García"));

        Patient patient = Patient.create(id, tenantId, demographics, NOW);

        assertThat(patient.id()).isEqualTo(id);
        assertThat(patient.tenantId()).isEqualTo(tenantId);
        assertThat(patient.demographics()).isEqualTo(demographics);
        assertThat(patient.externalIdentifiers().isEmpty()).isTrue();
        assertThat(patient.primaryOrganizationId()).isEmpty();
        assertThat(patient.status()).isEqualTo(PatientStatus.ACTIVE);
        assertThat(patient.createdAt()).isEqualTo(NOW);
        assertThat(patient.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldCreateWithPrimaryOrganizationAndExternalIdentifiers() {
        PrimaryOrganizationId orgId = PrimaryOrganizationId.of(UUID.randomUUID());
        ExternalIdentifiers identifiers = ExternalIdentifiers.of(
                java.util.List.of(ExternalIdentifier.of("MRN", "A-100"))
        );

        Patient patient = Patient.create(
                PatientId.generate(),
                TenantId.generate(),
                PatientDemographics.of(PatientDisplayName.of("Buddy")),
                identifiers,
                orgId,
                NOW
        );

        assertThat(patient.primaryOrganizationId()).contains(orgId);
        assertThat(patient.externalIdentifiers().size()).isEqualTo(1);
        assertThat(patient.status()).isEqualTo(PatientStatus.ACTIVE);
    }

    @Test
    void shouldRequireTenantId() {
        assertThatThrownBy(() -> Patient.create(
                PatientId.generate(),
                null,
                PatientDemographics.of(PatientDisplayName.of("Ana")),
                NOW
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void shouldKeepTenantIdImmutableAfterMutations() {
        Patient patient = activePatient();
        TenantId original = patient.tenantId();

        patient.updateDemographics(PatientDemographics.of(PatientDisplayName.of("Renamed")));
        patient.assignPrimaryOrganization(PrimaryOrganizationId.of(UUID.randomUUID()));
        patient.removePrimaryOrganization();
        patient.archive();
        patient.activate();

        assertThat(patient.tenantId()).isEqualTo(original);
    }

    @Test
    void shouldUpdateDemographicsWhenActive() {
        Patient patient = activePatient();

        PatientDemographics updated = PatientDemographics.of(
                PatientDisplayName.of("María López"),
                ContactEmail.of("maria@example.com"),
                ContactPhone.of("+593999000111"),
                DateOfBirth.of(LocalDate.of(1990, 5, 20))
        );
        patient.updateDemographics(updated);

        assertThat(patient.demographics()).isEqualTo(updated);
        assertThat(patient.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldReplaceExternalIdentifiersWhenActive() {
        Patient patient = activePatient();

        ExternalIdentifiers identifiers = ExternalIdentifiers.of(
                java.util.List.of(
                        ExternalIdentifier.of("NATIONAL_ID", "1712345678"),
                        ExternalIdentifier.of("MRN", "P-22")
                )
        );
        patient.replaceExternalIdentifiers(identifiers);

        assertThat(patient.externalIdentifiers()).isEqualTo(identifiers);
        assertThat(patient.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldAssignAndRemovePrimaryOrganization() {
        Patient patient = activePatient();
        PrimaryOrganizationId orgId = PrimaryOrganizationId.of(UUID.randomUUID());

        patient.assignPrimaryOrganization(orgId);
        assertThat(patient.primaryOrganizationId()).contains(orgId);

        patient.removePrimaryOrganization();
        assertThat(patient.primaryOrganizationId()).isEmpty();
        assertThat(patient.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldRejectNullPrimaryOrganizationOnAssign() {
        Patient patient = activePatient();

        assertThatThrownBy(() -> patient.assignPrimaryOrganization(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("organizationId");
    }

    @Test
    void shouldArchiveAndActivate() {
        Patient patient = activePatient();

        patient.archive();
        assertThat(patient.status()).isEqualTo(PatientStatus.ARCHIVED);

        patient.activate();
        assertThat(patient.status()).isEqualTo(PatientStatus.ACTIVE);
        assertThat(patient.updatedAt()).isAfter(NOW);
    }

    @Test
    void shouldRejectArchiveWhenAlreadyArchived() {
        Patient patient = activePatient();
        patient.archive();

        assertThatThrownBy(patient::archive)
                .isInstanceOf(InvalidPatientStateException.class)
                .hasMessageContaining("already archived");
    }

    @Test
    void shouldRejectActivateWhenAlreadyActive() {
        Patient patient = activePatient();

        assertThatThrownBy(patient::activate)
                .isInstanceOf(InvalidPatientStateException.class)
                .hasMessageContaining("already active");
    }

    @Test
    void shouldRejectDemographicUpdateWhenArchived() {
        Patient patient = activePatient();
        patient.archive();

        assertThatThrownBy(() -> patient.updateDemographics(
                PatientDemographics.of(PatientDisplayName.of("X"))
        )).isInstanceOf(InvalidPatientStateException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void shouldRejectPrimaryOrganizationMutationWhenArchived() {
        Patient patient = activePatient();
        patient.archive();

        assertThatThrownBy(() -> patient.assignPrimaryOrganization(PrimaryOrganizationId.of(UUID.randomUUID())))
                .isInstanceOf(InvalidPatientStateException.class)
                .hasMessageContaining("archived");

        assertThatThrownBy(patient::removePrimaryOrganization)
                .isInstanceOf(InvalidPatientStateException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void shouldRejectExternalIdentifierReplaceWhenArchived() {
        Patient patient = activePatient();
        patient.archive();

        assertThatThrownBy(() -> patient.replaceExternalIdentifiers(ExternalIdentifiers.empty()))
                .isInstanceOf(InvalidPatientStateException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void shouldReconstitutePatient() {
        PatientId id = PatientId.generate();
        TenantId tenantId = TenantId.generate();
        PatientDemographics demographics = PatientDemographics.of(PatientDisplayName.of("Historic"));
        PrimaryOrganizationId orgId = PrimaryOrganizationId.of(UUID.randomUUID());
        Instant createdAt = NOW.minusSeconds(3600);
        Instant updatedAt = NOW.minusSeconds(60);

        Patient patient = Patient.reconstitute(
                id,
                tenantId,
                demographics,
                ExternalIdentifiers.empty(),
                orgId,
                PatientStatus.ARCHIVED,
                createdAt,
                updatedAt
        );

        assertThat(patient.id()).isEqualTo(id);
        assertThat(patient.tenantId()).isEqualTo(tenantId);
        assertThat(patient.demographics()).isEqualTo(demographics);
        assertThat(patient.primaryOrganizationId()).contains(orgId);
        assertThat(patient.status()).isEqualTo(PatientStatus.ARCHIVED);
        assertThat(patient.createdAt()).isEqualTo(createdAt);
        assertThat(patient.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldNeverExposeOfficeOrOperationalConcernsInPublicApi() {
        assertThat(Patient.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain(
                        "assignOffice",
                        "setOfficeId",
                        "addAppointment",
                        "addEncounter",
                        "addMedicalRecord",
                        "assignStaff",
                        "linkIdentity",
                        "linkMembership"
                );
    }

    private static Patient activePatient() {
        return Patient.create(
                PatientId.generate(),
                TenantId.generate(),
                PatientDemographics.of(PatientDisplayName.of("María García")),
                NOW
        );
    }
}
