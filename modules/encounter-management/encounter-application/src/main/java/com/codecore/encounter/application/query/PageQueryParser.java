package com.codecore.encounter.application.query;

import java.util.Map;
import java.util.Set;

public final class PageQueryParser {

    private static final Map<String, String> ENCOUNTER_SORT_COLUMNS = Map.of(
            "startedAt", "started_at",
            "endedAt", "ended_at",
            "status", "status",
            "createdAt", "created_at",
            "updatedAt", "updated_at"
    );

    private static final Set<String> ENCOUNTER_ALLOWED_SORT = ENCOUNTER_SORT_COLUMNS.keySet();

    private PageQueryParser() {
    }

    public static PageQuery parseEncounterPageQuery(int page, int size, String sort) {
        String sortField = "startedAt";
        PageQuery.SortDirection direction = PageQuery.SortDirection.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String candidate = parts[0].trim();
            if (ENCOUNTER_ALLOWED_SORT.contains(candidate)) {
                sortField = candidate;
            }
            if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                direction = PageQuery.SortDirection.DESC;
            } else if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = PageQuery.SortDirection.ASC;
            }
        }
        int safeSize = Math.min(Math.max(size, 1), PageQuery.MAX_SIZE);
        int safePage = Math.max(page, 0);
        return new PageQuery(safePage, safeSize, sortField, direction);
    }

    public static String encounterSqlOrderColumn(String sortField) {
        return ENCOUNTER_SORT_COLUMNS.getOrDefault(sortField, ENCOUNTER_SORT_COLUMNS.get("startedAt"));
    }
}
