package com.codecore.appointment.contract.authorization;

import java.util.Set;

/**
 * Scheduling (Appointment) permission contract (FASE 18.5).
 * <p>
 * String codes are the canonical identifiers seeded in {@code iam.permission}.
 * IAM maps them to {@code PermissionCode} via {@code IamPermissionCatalog}.
 * <p>
 * Intentionally limited to planned-commitment lifecycle — no vertical-specific verbs (ADR-014).
 * {@code complete} maps to {@link #APPOINTMENT_UPDATE} (mirror Patient activate → update).
 */
public final class AppointmentPermissionCatalog {

    public static final String APPOINTMENT_CREATE = "appointment:create";
    public static final String APPOINTMENT_READ = "appointment:read";
    public static final String APPOINTMENT_UPDATE = "appointment:update";
    public static final String APPOINTMENT_CANCEL = "appointment:cancel";

    /** Full Appointment planned-commitment lifecycle contract (FASE 18 — no encounter/slots/billing). */
    public static final Set<String> ALL = Set.of(
            APPOINTMENT_CREATE,
            APPOINTMENT_READ,
            APPOINTMENT_UPDATE,
            APPOINTMENT_CANCEL
    );

    /** Read-only consultation of planned commitments. */
    public static final Set<String> APPOINTMENT_READ_ONLY = Set.of(APPOINTMENT_READ);

    private AppointmentPermissionCatalog() {
    }
}
