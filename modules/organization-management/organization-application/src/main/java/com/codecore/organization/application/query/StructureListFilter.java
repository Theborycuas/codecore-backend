package com.codecore.organization.application.query;

public enum StructureListFilter {
    ACTIVE,
    ARCHIVED,
    ALL;

    public static StructureListFilter parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ACTIVE;
        }
        return StructureListFilter.valueOf(raw.trim().toUpperCase());
    }
}
