package com.codecore.audit.application.query;

import java.util.Map;
import java.util.Set;

public final class PageQueryParser {

    private static final Map<String, String> AUDIT_SORT_COLUMNS = Map.of(
            "occurredAt", "occurred_at",
            "createdAt", "created_at",
            "actionCode", "action_code"
    );

    private static final Set<String> AUDIT_ALLOWED_SORT = AUDIT_SORT_COLUMNS.keySet();

    private PageQueryParser() {
    }

    public static PageQuery parseAuditPageQuery(int page, int size, String sort) {
        String sortField = "occurredAt";
        PageQuery.SortDirection direction = PageQuery.SortDirection.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String candidate = parts[0].trim();
            if (AUDIT_ALLOWED_SORT.contains(candidate)) {
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

    public static String auditSqlOrderColumn(String sortField) {
        return AUDIT_SORT_COLUMNS.getOrDefault(sortField, AUDIT_SORT_COLUMNS.get("occurredAt"));
    }
}
