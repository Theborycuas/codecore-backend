package com.codecore.encounter.infrastructure.persistence.repository;

import com.codecore.encounter.application.port.out.EncounterQueryPort;
import com.codecore.encounter.application.port.out.EncounterRepository;
import com.codecore.encounter.domain.model.encounter.Encounter;
import com.codecore.encounter.domain.valueobject.AppointmentId;
import com.codecore.encounter.domain.valueobject.EncounterId;
import com.codecore.encounter.domain.valueobject.EncounterStatus;
import com.codecore.encounter.domain.valueobject.OfficeId;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(EncounterPersistenceTestConfiguration.class)
class R2dbcEncounterRepositoryIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-11T22:00:00Z");
    private static final Instant STARTED = Instant.parse("2026-07-12T14:00:00Z");
    private static final Instant ENDED = Instant.parse("2026-07-12T15:00:00Z");

    @Autowired
    private EncounterRepository encounterRepository;

    @Autowired
    private EncounterQueryPort encounterQueryPort;

    @Test
    void shouldPersistAndFindById() {
        EncounterId encounterId = EncounterId.generate();
        TenantId tenantId = TenantId.generate();
        PatientId patientId = PatientId.of(UUID.randomUUID());
        StaffAssignmentId staffAssignmentId = StaffAssignmentId.of(UUID.randomUUID());
        OrganizationId organizationId = OrganizationId.of(UUID.randomUUID());
        OfficeId officeId = OfficeId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());

        Encounter encounter = Encounter.open(
                encounterId,
                tenantId,
                patientId,
                staffAssignmentId,
                organizationId,
                officeId,
                appointmentId,
                STARTED,
                NOW
        );

        StepVerifier.create(encounterRepository.save(encounter))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(encounterId);
                    assertThat(saved.tenantId()).isEqualTo(tenantId);
                    assertThat(saved.patientId()).isEqualTo(patientId);
                    assertThat(saved.staffAssignmentId()).isEqualTo(staffAssignmentId);
                    assertThat(saved.organizationId()).isEqualTo(organizationId);
                    assertThat(saved.officeId()).contains(officeId);
                    assertThat(saved.appointmentId()).contains(appointmentId);
                    assertThat(saved.startedAt()).isEqualTo(STARTED);
                    assertThat(saved.endedAt()).isEmpty();
                    assertThat(saved.status()).isEqualTo(EncounterStatus.IN_PROGRESS);
                })
                .verifyComplete();

        StepVerifier.create(encounterRepository.findById(encounterId))
                .assertNext(found -> {
                    assertThat(found.id()).isEqualTo(encounterId);
                    assertThat(found.appointmentId()).contains(appointmentId);
                    assertThat(found.status()).isEqualTo(EncounterStatus.IN_PROGRESS);
                })
                .verifyComplete();
    }

    @Test
    void shouldReportExistsByIdAndTenant() {
        EncounterId encounterId = EncounterId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(encounterRepository.save(openEncounter(encounterId, tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterRepository.existsById(encounterId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(encounterRepository.existsByIdAndTenantId(encounterId, tenantId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(encounterRepository.existsByIdAndTenantId(encounterId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(encounterRepository.existsById(EncounterId.generate()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldCountAndFindByTenantId() {
        TenantId tenantId = TenantId.generate();
        TenantId otherTenantId = TenantId.generate();

        StepVerifier.create(encounterRepository.save(openEncounter(EncounterId.generate(), tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(encounterRepository.save(openEncounter(EncounterId.generate(), tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(encounterRepository.save(openEncounter(EncounterId.generate(), otherTenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterQueryPort.countByTenantId(tenantId))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(encounterQueryPort.findByTenantId(tenantId))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldIsolateCrossTenantReads() {
        EncounterId encounterId = EncounterId.generate();
        TenantId tenantId = TenantId.generate();
        TenantId otherTenantId = TenantId.generate();

        StepVerifier.create(encounterRepository.save(openEncounter(encounterId, tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterQueryPort.findByIdAndTenantId(encounterId, tenantId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(encounterId))
                .verifyComplete();

        StepVerifier.create(encounterQueryPort.findByIdAndTenantId(encounterId, otherTenantId))
                .verifyComplete();
    }

    @Test
    void shouldPersistNullAndNonNullOfficeAndAppointment() {
        TenantId tenantId = TenantId.generate();
        OfficeId officeId = OfficeId.of(UUID.randomUUID());
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());

        StepVerifier.create(encounterRepository.save(
                        openEncounter(EncounterId.generate(), tenantId, null, null)))
                .assertNext(saved -> {
                    assertThat(saved.officeId()).isEmpty();
                    assertThat(saved.appointmentId()).isEmpty();
                })
                .verifyComplete();

        StepVerifier.create(encounterRepository.save(
                        openEncounter(EncounterId.generate(), tenantId, officeId, appointmentId)))
                .assertNext(saved -> {
                    assertThat(saved.officeId()).contains(officeId);
                    assertThat(saved.appointmentId()).contains(appointmentId);
                })
                .verifyComplete();
    }

    @Test
    void shouldFindByTenantIdAndOrganizationId() {
        TenantId tenantId = TenantId.generate();
        OrganizationId organizationId = OrganizationId.of(UUID.randomUUID());
        EncounterId matchingId = EncounterId.generate();

        Encounter matching = Encounter.open(
                matchingId,
                tenantId,
                PatientId.of(UUID.randomUUID()),
                StaffAssignmentId.of(UUID.randomUUID()),
                organizationId,
                null,
                null,
                STARTED,
                NOW
        );

        StepVerifier.create(encounterRepository.save(matching))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(encounterRepository.save(openEncounter(EncounterId.generate(), tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterQueryPort.findByTenantIdAndOrganizationId(tenantId, organizationId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(matchingId))
                .verifyComplete();
    }

    @Test
    void shouldFindByTenantIdAndPatientId() {
        TenantId tenantId = TenantId.generate();
        PatientId patientId = PatientId.of(UUID.randomUUID());
        EncounterId matchingId = EncounterId.generate();

        Encounter matching = Encounter.open(
                matchingId,
                tenantId,
                patientId,
                StaffAssignmentId.of(UUID.randomUUID()),
                OrganizationId.of(UUID.randomUUID()),
                null,
                null,
                STARTED,
                NOW
        );

        StepVerifier.create(encounterRepository.save(matching))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterQueryPort.findByTenantIdAndPatientId(tenantId, patientId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(matchingId))
                .verifyComplete();
    }

    @Test
    void shouldFindByTenantIdAndStaffAssignmentId() {
        TenantId tenantId = TenantId.generate();
        StaffAssignmentId staffAssignmentId = StaffAssignmentId.of(UUID.randomUUID());
        EncounterId matchingId = EncounterId.generate();

        Encounter matching = Encounter.open(
                matchingId,
                tenantId,
                PatientId.of(UUID.randomUUID()),
                staffAssignmentId,
                OrganizationId.of(UUID.randomUUID()),
                null,
                null,
                STARTED,
                NOW
        );

        StepVerifier.create(encounterRepository.save(matching))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterQueryPort.findByTenantIdAndStaffAssignmentId(tenantId, staffAssignmentId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(matchingId))
                .verifyComplete();
    }

    @Test
    void shouldFindByTenantIdAndAppointmentId() {
        TenantId tenantId = TenantId.generate();
        AppointmentId appointmentId = AppointmentId.of(UUID.randomUUID());
        EncounterId matchingId = EncounterId.generate();

        Encounter matching = Encounter.open(
                matchingId,
                tenantId,
                PatientId.of(UUID.randomUUID()),
                StaffAssignmentId.of(UUID.randomUUID()),
                OrganizationId.of(UUID.randomUUID()),
                null,
                appointmentId,
                STARTED,
                NOW
        );

        StepVerifier.create(encounterRepository.save(matching))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterQueryPort.findByTenantIdAndAppointmentId(tenantId, appointmentId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(matchingId))
                .verifyComplete();
    }

    @Test
    void shouldFindByTenantIdAndStatus() {
        TenantId tenantId = TenantId.generate();
        EncounterId inProgressId = EncounterId.generate();
        EncounterId completedId = EncounterId.generate();

        StepVerifier.create(encounterRepository.save(openEncounter(inProgressId, tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        Encounter toComplete = openEncounter(completedId, tenantId, null, null);
        toComplete.complete(ENDED);
        StepVerifier.create(encounterRepository.save(toComplete))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterQueryPort.findByTenantIdAndStatus(tenantId, EncounterStatus.IN_PROGRESS))
                .assertNext(found -> assertThat(found.id()).isEqualTo(inProgressId))
                .verifyComplete();

        StepVerifier.create(encounterQueryPort.findByTenantIdAndStatus(tenantId, EncounterStatus.COMPLETED))
                .assertNext(found -> {
                    assertThat(found.id()).isEqualTo(completedId);
                    assertThat(found.endedAt()).contains(ENDED);
                })
                .verifyComplete();
    }

    @Test
    void shouldUpdateWithoutDuplicatingRow() {
        EncounterId encounterId = EncounterId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(encounterRepository.save(openEncounter(encounterId, tenantId, null, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(encounterRepository.findById(encounterId)
                        .flatMap(loaded -> {
                            loaded.complete(ENDED);
                            return encounterRepository.save(loaded);
                        }))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(encounterId);
                    assertThat(saved.status()).isEqualTo(EncounterStatus.COMPLETED);
                    assertThat(saved.endedAt()).contains(ENDED);
                })
                .verifyComplete();

        StepVerifier.create(encounterQueryPort.countByTenantId(tenantId))
                .expectNext(1L)
                .verifyComplete();
    }

    private static Encounter openEncounter(
            EncounterId encounterId,
            TenantId tenantId,
            OfficeId officeId,
            AppointmentId appointmentId
    ) {
        return Encounter.open(
                encounterId,
                tenantId,
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
