package com.codecore.payment.application.query;

import java.util.Map;
import java.util.Set;

public final class PageQueryParser {

    private static final Map<String, String> PAYMENT_SORT_COLUMNS = Map.of(
            "recordedAt", "recorded_at",
            "status", "status",
            "createdAt", "created_at",
            "updatedAt", "updated_at"
    );

    private static final Set<String> PAYMENT_ALLOWED_SORT = PAYMENT_SORT_COLUMNS.keySet();

    private PageQueryParser() {
    }

    public static PageQuery parsePaymentPageQuery(int page, int size, String sort) {
        String sortField = "recordedAt";
        PageQuery.SortDirection direction = PageQuery.SortDirection.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String candidate = parts[0].trim();
            if (PAYMENT_ALLOWED_SORT.contains(candidate)) {
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

    public static String paymentSqlOrderColumn(String sortField) {
        return PAYMENT_SORT_COLUMNS.getOrDefault(sortField, PAYMENT_SORT_COLUMNS.get("recordedAt"));
    }
}
