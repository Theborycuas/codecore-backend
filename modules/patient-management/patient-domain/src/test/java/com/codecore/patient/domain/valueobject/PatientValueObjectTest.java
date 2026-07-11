package com.codecore.patient.domain.valueobject;

import com.codecore.patient.domain.exception.InvalidDomainValueException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientValueObjectTest {

    @Test
    void patientIdShouldSupportEqualityAndGeneration() {
        UUID uuid = UUID.randomUUID();
        PatientId a = new PatientId(uuid);
        PatientId b = new PatientId(uuid.toString());

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.asString()).isEqualTo(uuid.toString());
        assertThat(PatientId.generate()).isNotEqualTo(a);
    }

    @Test
    void tenantIdShouldSupportEquality() {
        UUID uuid = UUID.randomUUID();
        assertThat(new TenantId(uuid)).isEqualTo(new TenantId(uuid.toString()));
    }

    @Test
    void primaryOrganizationIdShouldWrapUuid() {
        UUID uuid = UUID.randomUUID();
        PrimaryOrganizationId id = PrimaryOrganizationId.of(uuid);

        assertThat(id.value()).isEqualTo(uuid);
        assertThat(id).isEqualTo(new PrimaryOrganizationId(uuid.toString()));
    }

    @Test
    void displayNameShouldRejectBlankAndTooLong() {
        assertThat(PatientDisplayName.of("  Ana  ").value()).isEqualTo("Ana");

        assertThatThrownBy(() -> PatientDisplayName.of("  "))
                .isInstanceOf(InvalidDomainValueException.class);

        assertThatThrownBy(() -> PatientDisplayName.of("x".repeat(201)))
                .isInstanceOf(InvalidDomainValueException.class);
    }

    @Test
    void contactEmailShouldNormalizeAndValidate() {
        assertThat(ContactEmail.of("  Foo@Example.COM ").value()).isEqualTo("foo@example.com");

        assertThatThrownBy(() -> ContactEmail.of("not-an-email"))
                .isInstanceOf(InvalidDomainValueException.class);
    }

    @Test
    void contactPhoneShouldRejectBlank() {
        assertThat(ContactPhone.of(" +593999 ").value()).isEqualTo("+593999");

        assertThatThrownBy(() -> ContactPhone.of(" "))
                .isInstanceOf(InvalidDomainValueException.class);
    }

    @Test
    void dateOfBirthShouldRejectFuture() {
        assertThat(DateOfBirth.of(LocalDate.of(2000, 1, 1)).value())
                .isEqualTo(LocalDate.of(2000, 1, 1));

        assertThatThrownBy(() -> DateOfBirth.of(LocalDate.now().plusDays(1)))
                .isInstanceOf(InvalidDomainValueException.class);
    }

    @Test
    void externalIdentifierTypeShouldNormalize() {
        assertThat(ExternalIdentifierType.of("national-id").value()).isEqualTo("NATIONAL_ID");

        assertThatThrownBy(() -> ExternalIdentifierType.of("1BAD"))
                .isInstanceOf(InvalidDomainValueException.class);
    }

    @Test
    void externalIdentifiersShouldRejectDuplicateTypes() {
        ExternalIdentifier a = ExternalIdentifier.of("MRN", "1");
        ExternalIdentifier b = ExternalIdentifier.of("MRN", "2");

        assertThatThrownBy(() -> ExternalIdentifiers.of(List.of(a, b)))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void externalIdentifiersShouldPreserveLookup() {
        ExternalIdentifier mrn = ExternalIdentifier.of("MRN", "P-1");
        ExternalIdentifiers identifiers = ExternalIdentifiers.of(List.of(mrn));

        assertThat(identifiers.find(ExternalIdentifierType.of("MRN"))).contains(mrn);
        assertThat(identifiers.find(ExternalIdentifierType.of("CHIP"))).isEmpty();
        assertThat(identifiers).isEqualTo(ExternalIdentifiers.of(List.of(mrn)));
    }

    @Test
    void demographicsShouldCarryOptionalContacts() {
        PatientDemographics demographics = PatientDemographics.of(
                PatientDisplayName.of("Ana"),
                ContactEmail.of("ana@example.com"),
                null,
                DateOfBirth.of(LocalDate.of(1995, 3, 10))
        );

        assertThat(demographics.displayName().value()).isEqualTo("Ana");
        assertThat(demographics.email()).isPresent();
        assertThat(demographics.phone()).isEmpty();
        assertThat(demographics.dateOfBirth()).isPresent();
    }

    @Test
    void patientStatusShouldOnlyExposeActiveAndArchived() {
        assertThat(PatientStatus.values()).containsExactly(PatientStatus.ACTIVE, PatientStatus.ARCHIVED);
    }
}
