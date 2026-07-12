package com.codecore.encounter.infrastructure.adapters;

import com.codecore.encounter.application.port.out.EncounterRepository;
import com.codecore.encounter.contract.reference.EncounterReferencePort;
import com.codecore.encounter.contract.reference.EncounterReferenceView;
import com.codecore.encounter.domain.model.encounter.Encounter;
import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.EncounterStatus;
import com.codecore.encounter.domain.valueobject.OrganizationId;
import com.codecore.encounter.domain.valueobject.PatientId;
import com.codecore.encounter.domain.valueobject.StaffAssignmentId;
import com.codecore.encounter.domain.valueobject.TenantId;
import com.codecore.encounter.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.encounter.testsupport.EncounterPersistenceTestConfiguration;
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
        EncounterPersistenceTestConfiguration.class,
        R2dbcEncounterReferenceAdapter.class
})
class EncounterReferencePortIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-11T22:00:00Z");
    private static final Instant STARTED = Instant.parse("2026-07-12T14:00:00Z");
    private static final Instant ENDED = Instant.parse("2026-07-12T15:00:00Z");

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private EncounterReferencePort encounterReferencePort;

    @Test
    void shouldReturnTrueForInProgressEncounterInTenant() {
        EncounterId encounterId = EncounterId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(encounterRepository.save(open(encounterId, tenantId, PatientId.of(UUID.randomUUID()))))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterReferencePort.existsInProgressByIdAndTenant(encounterId, tenantId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseForWrongTenantOrUnknownId() {
        EncounterId encounterId = EncounterId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(encounterRepository.save(open(encounterId, tenantId, PatientId.of(UUID.randomUUID()))))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterReferencePort.existsInProgressByIdAndTenant(encounterId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(encounterReferencePort.existsInProgressByIdAndTenant(EncounterId.generate(), tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenEncounterCancelled() {
        EncounterId encounterId = EncounterId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(encounterRepository.save(open(encounterId, tenantId, PatientId.of(UUID.randomUUID())))
                        .flatMap(saved -> {
                            saved.cancel();
                            return encounterRepository.save(saved);
                        }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterReferencePort.existsInProgressByIdAndTenant(encounterId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenEncounterCompletedForExistsInProgress() {
        EncounterId encounterId = EncounterId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(encounterRepository.save(open(encounterId, tenantId, PatientId.of(UUID.randomUUID())))
                        .flatMap(saved -> {
                            saved.complete(ENDED);
                            return encounterRepository.save(saved);
                        }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterReferencePort.existsInProgressByIdAndTenant(encounterId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldFindLinkableInProgressWithPatientId() {
        EncounterId encounterId = EncounterId.generate();
        TenantId tenantId = TenantId.generate();
        PatientId patientId = PatientId.of(UUID.randomUUID());

        StepVerifier.create(encounterRepository.save(open(encounterId, tenantId, patientId)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterReferencePort.findLinkableByIdAndTenant(encounterId, tenantId))
                .assertNext(optional -> {
                    assertThat(optional).isPresent();
                    EncounterReferenceView view = optional.get();
                    assertThat(view.encounterId()).isEqualTo(encounterId);
                    assertThat(view.patientId()).isEqualTo(patientId);
                    assertThat(view.status()).isEqualTo(EncounterStatus.IN_PROGRESS);
                    assertThat(view.isLinkableForClinicalDocs()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void shouldFindLinkableCompletedWithPatientId() {
        EncounterId encounterId = EncounterId.generate();
        TenantId tenantId = TenantId.generate();
        PatientId patientId = PatientId.of(UUID.randomUUID());

        StepVerifier.create(encounterRepository.save(open(encounterId, tenantId, patientId))
                        .flatMap(saved -> {
                            saved.complete(ENDED);
                            return encounterRepository.save(saved);
                        }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterReferencePort.findLinkableByIdAndTenant(encounterId, tenantId))
                .assertNext(optional -> {
                    assertThat(optional).isPresent();
                    assertThat(optional.get().status()).isEqualTo(EncounterStatus.COMPLETED);
                    assertThat(optional.get().patientId()).isEqualTo(patientId);
                })
                .verifyComplete();

        StepVerifier.create(encounterReferencePort.existsInProgressByIdAndTenant(encounterId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyLinkableWhenCancelledOrUnknown() {
        EncounterId encounterId = EncounterId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(encounterRepository.save(open(encounterId, tenantId, PatientId.of(UUID.randomUUID())))
                        .flatMap(saved -> {
                            saved.cancel();
                            return encounterRepository.save(saved);
                        }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterReferencePort.findLinkableByIdAndTenant(encounterId, tenantId))
                .expectNext(Optional.empty())
                .verifyComplete();

        StepVerifier.create(encounterReferencePort.findLinkableByIdAndTenant(EncounterId.generate(), tenantId))
                .expectNext(Optional.empty())
                .verifyComplete();
    }

    private static Encounter open(EncounterId encounterId, TenantId tenantId, PatientId patientId) {
        return Encounter.open(
                encounterId,
                tenantId,
                patientId,
                StaffAssignmentId.of(UUID.randomUUID()),
                OrganizationId.of(UUID.randomUUID()),
                null,
                null,
                STARTED,
                NOW
        );
    }
}
