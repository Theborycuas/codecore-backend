package com.codecore.patient.infrastructure.persistence.repository;

import com.codecore.patient.application.port.out.PatientQueryPort;
import com.codecore.patient.application.port.out.PatientRepository;
import com.codecore.patient.domain.model.patient.Patient;
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
import com.codecore.patient.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.patient.testsupport.PatientPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(PatientPersistenceTestConfiguration.class)
class R2dbcPatientRepositoryIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-11T16:00:00Z");

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientQueryPort patientQueryPort;

    @Test
    void shouldPersistAndFindById() {
        PatientId patientId = PatientId.generate();
        TenantId tenantId = TenantId.generate();
        PrimaryOrganizationId orgId = PrimaryOrganizationId.of(UUID.randomUUID());
        Patient patient = Patient.create(
                patientId,
                tenantId,
                PatientDemographics.of(
                        PatientDisplayName.of("María García"),
                        ContactEmail.of("maria@example.com"),
                        ContactPhone.of("+593999000111"),
                        DateOfBirth.of(LocalDate.of(1990, 5, 20))
                ),
                ExternalIdentifiers.of(List.of(ExternalIdentifier.of("MRN", "P-100"))),
                orgId,
                NOW
        );

        StepVerifier.create(patientRepository.save(patient))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(patientId);
                    assertThat(saved.tenantId()).isEqualTo(tenantId);
                    assertThat(saved.demographics().displayName().value()).isEqualTo("María García");
                    assertThat(saved.demographics().email()).isPresent();
                    assertThat(saved.demographics().phone()).isPresent();
                    assertThat(saved.demographics().dateOfBirth()).isPresent();
                    assertThat(saved.primaryOrganizationId()).contains(orgId);
                    assertThat(saved.externalIdentifiers().size()).isEqualTo(1);
                    assertThat(saved.status()).isEqualTo(PatientStatus.ACTIVE);
                })
                .verifyComplete();

        StepVerifier.create(patientRepository.findById(patientId))
                .assertNext(found -> {
                    assertThat(found.id()).isEqualTo(patientId);
                    assertThat(found.primaryOrganizationId()).contains(orgId);
                    assertThat(found.externalIdentifiers().find(
                            com.codecore.patient.domain.valueobject.ExternalIdentifierType.of("MRN")
                    )).isPresent();
                })
                .verifyComplete();
    }

    @Test
    void shouldReportExistsByIdAndTenant() {
        PatientId patientId = PatientId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(patientRepository.save(activePatient(patientId, tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(patientRepository.existsById(patientId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(patientRepository.existsByIdAndTenantId(patientId, tenantId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(patientRepository.existsByIdAndTenantId(patientId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(patientRepository.existsById(PatientId.generate()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldCountAndFindByTenantId() {
        TenantId tenantId = TenantId.generate();
        TenantId otherTenantId = TenantId.generate();

        StepVerifier.create(patientRepository.save(activePatient(PatientId.generate(), tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(patientRepository.save(activePatient(PatientId.generate(), tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(patientRepository.save(activePatient(PatientId.generate(), otherTenantId, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(patientQueryPort.countByTenantId(tenantId))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(patientQueryPort.findByTenantId(tenantId))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldIsolateCrossTenantReads() {
        PatientId patientId = PatientId.generate();
        TenantId tenantId = TenantId.generate();
        TenantId otherTenantId = TenantId.generate();

        StepVerifier.create(patientRepository.save(activePatient(patientId, tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(patientQueryPort.findByIdAndTenantId(patientId, tenantId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(patientId))
                .verifyComplete();

        StepVerifier.create(patientQueryPort.findByIdAndTenantId(patientId, otherTenantId))
                .verifyComplete();
    }

    @Test
    void shouldPersistAndReplaceExternalIdentifiers() {
        PatientId patientId = PatientId.generate();
        TenantId tenantId = TenantId.generate();
        Patient patient = Patient.create(
                patientId,
                tenantId,
                PatientDemographics.of(PatientDisplayName.of("Buddy")),
                ExternalIdentifiers.of(List.of(
                        ExternalIdentifier.of("MICROCHIP", "CHIP-1"),
                        ExternalIdentifier.of("MRN", "V-9")
                )),
                null,
                NOW
        );

        StepVerifier.create(patientRepository.save(patient))
                .assertNext(saved -> assertThat(saved.externalIdentifiers().size()).isEqualTo(2))
                .verifyComplete();

        Patient updated = Patient.reconstitute(
                patientId,
                tenantId,
                PatientDemographics.of(PatientDisplayName.of("Buddy")),
                ExternalIdentifiers.of(List.of(ExternalIdentifier.of("MRN", "V-9-UPDATED"))),
                null,
                PatientStatus.ACTIVE,
                NOW,
                NOW
        );

        StepVerifier.create(patientRepository.save(updated))
                .assertNext(saved -> {
                    assertThat(saved.externalIdentifiers().size()).isEqualTo(1);
                    assertThat(saved.externalIdentifiers().find(
                            com.codecore.patient.domain.valueobject.ExternalIdentifierType.of("MRN")
                    )).map(ExternalIdentifier::value).contains("V-9-UPDATED");
                    assertThat(saved.externalIdentifiers().find(
                            com.codecore.patient.domain.valueobject.ExternalIdentifierType.of("MICROCHIP")
                    )).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void shouldPersistNullAndNonNullPrimaryOrganizationId() {
        TenantId tenantId = TenantId.generate();
        PrimaryOrganizationId orgId = PrimaryOrganizationId.of(UUID.randomUUID());

        PatientId withoutOrg = PatientId.generate();
        PatientId withOrg = PatientId.generate();

        StepVerifier.create(patientRepository.save(activePatient(withoutOrg, tenantId, null)))
                .assertNext(saved -> assertThat(saved.primaryOrganizationId()).isEmpty())
                .verifyComplete();

        StepVerifier.create(patientRepository.save(activePatient(withOrg, tenantId, orgId)))
                .assertNext(saved -> assertThat(saved.primaryOrganizationId()).contains(orgId))
                .verifyComplete();

        StepVerifier.create(patientQueryPort.findByTenantIdAndPrimaryOrganizationId(tenantId, orgId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(withOrg))
                .verifyComplete();
    }

    @Test
    void shouldFindByTenantIdAndStatus() {
        TenantId tenantId = TenantId.generate();
        PatientId activeId = PatientId.generate();
        PatientId archivedId = PatientId.generate();

        StepVerifier.create(patientRepository.save(activePatient(activeId, tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();

        Patient archived = Patient.create(
                archivedId,
                tenantId,
                PatientDemographics.of(PatientDisplayName.of("Archived")),
                NOW
        );
        archived.archive();

        StepVerifier.create(patientRepository.save(archived))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(patientQueryPort.findByTenantIdAndStatus(tenantId, PatientStatus.ACTIVE))
                .assertNext(found -> assertThat(found.id()).isEqualTo(activeId))
                .verifyComplete();

        StepVerifier.create(patientQueryPort.findByTenantIdAndStatus(tenantId, PatientStatus.ARCHIVED))
                .assertNext(found -> assertThat(found.id()).isEqualTo(archivedId))
                .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateExternalIdentifierWithinSameTenant() {
        TenantId tenantId = TenantId.generate();
        ExternalIdentifiers shared = ExternalIdentifiers.of(
                List.of(ExternalIdentifier.of("NATIONAL_ID", "1712345678"))
        );

        StepVerifier.create(patientRepository.save(Patient.create(
                PatientId.generate(),
                tenantId,
                PatientDemographics.of(PatientDisplayName.of("One")),
                shared,
                null,
                NOW
        )))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(patientRepository.save(Patient.create(
                PatientId.generate(),
                tenantId,
                PatientDemographics.of(PatientDisplayName.of("Two")),
                shared,
                null,
                NOW
        )))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    @Test
    void shouldAllowSameExternalIdentifierInDifferentTenants() {
        ExternalIdentifiers shared = ExternalIdentifiers.of(
                List.of(ExternalIdentifier.of("MRN", "SHARED-1"))
        );

        StepVerifier.create(patientRepository.save(Patient.create(
                PatientId.generate(),
                TenantId.generate(),
                PatientDemographics.of(PatientDisplayName.of("Tenant A")),
                shared,
                null,
                NOW
        )))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(patientRepository.save(Patient.create(
                PatientId.generate(),
                TenantId.generate(),
                PatientDemographics.of(PatientDisplayName.of("Tenant B")),
                shared,
                null,
                NOW
        )))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldUpdateExistingPatientWithoutDuplicatingRow() {
        PatientId patientId = PatientId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(patientRepository.save(activePatient(patientId, tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();

        Patient renamed = Patient.create(
                patientId,
                tenantId,
                PatientDemographics.of(PatientDisplayName.of("Updated Name")),
                NOW
        );

        StepVerifier.create(patientRepository.save(renamed))
                .assertNext(saved -> assertThat(saved.demographics().displayName().value())
                        .isEqualTo("Updated Name"))
                .verifyComplete();

        StepVerifier.create(patientQueryPort.countByTenantId(tenantId))
                .expectNext(1L)
                .verifyComplete();
    }

    private static Patient activePatient(
            PatientId patientId,
            TenantId tenantId,
            PrimaryOrganizationId primaryOrganizationId
    ) {
        return Patient.create(
                patientId,
                tenantId,
                PatientDemographics.of(PatientDisplayName.of("Patient " + patientId.asString().substring(0, 8))),
                ExternalIdentifiers.empty(),
                primaryOrganizationId,
                NOW
        );
    }
}
