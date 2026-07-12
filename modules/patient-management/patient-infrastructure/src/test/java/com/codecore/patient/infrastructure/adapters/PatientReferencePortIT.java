package com.codecore.patient.infrastructure.adapters;

import com.codecore.patient.application.port.out.PatientRepository;
import com.codecore.patient.contract.reference.PatientReferencePort;
import com.codecore.patient.domain.model.patient.Patient;
import com.codecore.patient.domain.valueobject.PatientDemographics;
import com.codecore.patient.domain.valueobject.PatientDisplayName;
import com.codecore.patient.domain.valueobject.PatientId;
import com.codecore.patient.domain.valueobject.TenantId;
import com.codecore.patient.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.patient.testsupport.PatientPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Instant;

@DataR2dbcTest
@Import({
        PatientPersistenceTestConfiguration.class,
        R2dbcPatientReferenceAdapter.class
})
class PatientReferencePortIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-11T21:00:00Z");

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientReferencePort patientReferencePort;

    @Test
    void shouldReturnTrueForActivePatientInTenant() {
        PatientId patientId = PatientId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(patientRepository.save(Patient.create(
                        patientId,
                        tenantId,
                        PatientDemographics.of(PatientDisplayName.of("Active Subject"), null, null, null),
                        NOW
                )))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(patientReferencePort.existsActiveByIdAndTenant(patientId, tenantId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseForWrongTenantOrUnknownId() {
        PatientId patientId = PatientId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(patientRepository.save(Patient.create(
                        patientId,
                        tenantId,
                        PatientDemographics.of(PatientDisplayName.of("Scoped Subject"), null, null, null),
                        NOW
                )))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(patientReferencePort.existsActiveByIdAndTenant(patientId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(patientReferencePort.existsActiveByIdAndTenant(PatientId.generate(), tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenPatientArchived() {
        PatientId patientId = PatientId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(patientRepository.save(Patient.create(
                        patientId,
                        tenantId,
                        PatientDemographics.of(PatientDisplayName.of("Archived Subject"), null, null, null),
                        NOW
                )).flatMap(saved -> {
                    saved.archive();
                    return patientRepository.save(saved);
                }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(patientReferencePort.existsActiveByIdAndTenant(patientId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }
}
