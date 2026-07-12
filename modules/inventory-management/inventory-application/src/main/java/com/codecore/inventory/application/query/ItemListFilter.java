package com.codecore.inventory.application.query;

public enum ItemListFilter {
    ACTIVE,
    ARCHIVED,
    ALL;

    public static ItemListFilter parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ACTIVE;
        }
        return ItemListFilter.valueOf(raw.trim().toUpperCase());
    }
}
