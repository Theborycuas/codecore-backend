package com.codecore.inventory.application.query;

import java.util.Map;
import java.util.Set;

public final class PageQueryParser {

    private static final Map<String, String> ITEM_SORT_COLUMNS = Map.of(
            "displayName", "display_name",
            "code", "code",
            "status", "status",
            "createdAt", "created_at",
            "updatedAt", "updated_at"
    );

    private static final Set<String> ITEM_ALLOWED_SORT = ITEM_SORT_COLUMNS.keySet();

    private PageQueryParser() {
    }

    public static PageQuery parseItemPageQuery(int page, int size, String sort) {
        String sortField = "createdAt";
        PageQuery.SortDirection direction = PageQuery.SortDirection.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String candidate = parts[0].trim();
            if (ITEM_ALLOWED_SORT.contains(candidate)) {
                sortField = candidate;
            }
            if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = PageQuery.SortDirection.ASC;
            } else if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                direction = PageQuery.SortDirection.DESC;
            }
        }
        int safeSize = Math.min(Math.max(size, 1), PageQuery.MAX_SIZE);
        int safePage = Math.max(page, 0);
        return new PageQuery(safePage, safeSize, sortField, direction);
    }

    public static String itemSqlOrderColumn(String sortField) {
        return ITEM_SORT_COLUMNS.getOrDefault(sortField, ITEM_SORT_COLUMNS.get("createdAt"));
    }
}
