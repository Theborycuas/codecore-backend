package com.codecore.encounter.application.query;

/**
 * List status filter for Encounter administration (PASO 19.5.1).
 */
public enum EncounterListFilter {
    IN_PROGRESS,
    CANCELLED,
    COMPLETED,
    ALL;

    public static EncounterListFilter parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return IN_PROGRESS;
        }
        return EncounterListFilter.valueOf(raw.trim().toUpperCase());
    }
}
