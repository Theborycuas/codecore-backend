package com.codecore.appointment.contract.reference;

import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference contract for Appointment (ADR-013 / ADR-014).
 * Read-only; minimal surface for consumer write-time invariants
 * (Encounter, Notifications, Billing, …).
 */
public interface AppointmentReferencePort {

    /**
     * {@code true} when the appointment exists in the tenant and status is {@code SCHEDULED}
     * (open commitment — default for new operational links).
     * <p>
     * {@code CANCELLED} and {@code COMPLETED} return {@code false}. Consumers that need
     * historical existence checks should request a separate {@code existsByIdAndTenant}
     * evolution only when a consumer invariant requires it (ADR-013).
     */
    Mono<Boolean> existsScheduledByIdAndTenant(AppointmentId appointmentId, TenantId tenantId);
}
