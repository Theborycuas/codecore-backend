package com.codecore.patient.application.query;

public enum PatientListFilter {
    ACTIVE,
    ARCHIVED,
    ALL;

    public static PatientListFilter parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ACTIVE;
        }
        return PatientListFilter.valueOf(raw.trim().toUpperCase());
    }
}
