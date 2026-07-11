package com.codecore.patient.contract.authorization;

import java.util.Set;

/**
 * Clinical Foundation (Patient) permission contract (FASE 17.5).
 * <p>
 * String codes are the canonical identifiers seeded in {@code iam.permission}.
 * IAM maps them to {@code PermissionCode} via {@code IamPermissionCatalog}.
 * <p>
 * Intentionally limited to registry lifecycle — no vertical-specific verbs (ADR-012).
 */
public final class PatientPermissionCatalog {

    public static final String PATIENT_CREATE = "patient:create";
    public static final String PATIENT_READ = "patient:read";
    public static final String PATIENT_UPDATE = "patient:update";
    public static final String PATIENT_ARCHIVE = "patient:archive";

    /** Full Patient registry lifecycle contract (FASE 17 — no appointment/records/billing). */
    public static final Set<String> ALL = Set.of(
            PATIENT_CREATE,
            PATIENT_READ,
            PATIENT_UPDATE,
            PATIENT_ARCHIVE
    );

    /** Read-only consultation of clinical registry identity. */
    public static final Set<String> PATIENT_READ_ONLY = Set.of(PATIENT_READ);

    private PatientPermissionCatalog() {
    }
}
