package com.codecore.patient.contract.reference;

import com.codecore.patient.domain.valueobject.PatientId;
import com.codecore.patient.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference contract for Patient (ADR-013).
 * Read-only; minimal surface for consumer write-time invariants (Appointment, MedicalRecord, Billing).
 */
public interface PatientReferencePort {

    /**
     * {@code true} when the patient exists in the tenant and status is {@code ACTIVE}.
     */
    Mono<Boolean> existsActiveByIdAndTenant(PatientId patientId, TenantId tenantId);
}
