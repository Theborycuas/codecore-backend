package com.codecore.appointment.contract.reference;

import com.codecore.appointment.domain.valueobject.AppointmentId;
import com.codecore.appointment.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Cross-BC reference contract for Appointment (ADR-013 / ADR-014 / ADR-015).
 * Read-only; minimal surface for consumer write-time invariants
 * (Encounter, Notifications, Billing, …).
 */
public interface AppointmentReferencePort {

    /**
     * {@code true} when the appointment exists in the tenant and status is {@code SCHEDULED}
     * (open commitment — default for reminders / open-commitment operational links).
     * <p>
     * {@code CANCELLED} and {@code COMPLETED} return {@code false}.
     */
    Mono<Boolean> existsScheduledByIdAndTenant(AppointmentId appointmentId, TenantId tenantId);

    /**
     * Minimal {@code patientId} + {@code status} when the appointment exists in the tenant
     * and is linkable for Encounter (status ∈ {@code SCHEDULED}, {@code COMPLETED}).
     * Empty for unknown id, wrong tenant, or {@code CANCELLED} (ADR-015 §7).
     */
    Mono<Optional<AppointmentReferenceView>> findLinkableByIdAndTenant(
            AppointmentId appointmentId,
            TenantId tenantId
    );
}
