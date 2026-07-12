package com.codecore.encounter.contract.authorization;

import java.util.Set;

/**
 * Clinical Records (Encounter) permission contract (FASE 19.5).
 * <p>
 * String codes are the canonical identifiers seeded in {@code iam.permission}.
 * IAM maps them to {@code PermissionCode} via {@code IamPermissionCatalog}.
 * <p>
 * Intentionally limited to occurred-care-episode lifecycle — no vertical-specific verbs (ADR-015).
 * {@code complete} maps to {@link #ENCOUNTER_UPDATE} (mirror Appointment complete → update).
 */
public final class EncounterPermissionCatalog {

    public static final String ENCOUNTER_CREATE = "encounter:create";
    public static final String ENCOUNTER_READ = "encounter:read";
    public static final String ENCOUNTER_UPDATE = "encounter:update";
    public static final String ENCOUNTER_CANCEL = "encounter:cancel";

    /** Full Encounter occurred-episode lifecycle contract (FASE 19 — no notes/SOAP/odontogram/billing). */
    public static final Set<String> ALL = Set.of(
            ENCOUNTER_CREATE,
            ENCOUNTER_READ,
            ENCOUNTER_UPDATE,
            ENCOUNTER_CANCEL
    );

    /** Read-only consultation of occurred care episodes. */
    public static final Set<String> ENCOUNTER_READ_ONLY = Set.of(ENCOUNTER_READ);

    private EncounterPermissionCatalog() {
    }
}
