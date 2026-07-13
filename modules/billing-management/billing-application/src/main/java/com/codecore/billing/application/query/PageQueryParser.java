package com.codecore.billing.application.query;

import java.util.Map;
import java.util.Set;

public final class PageQueryParser {

    private static final Map<String, String> INVOICE_SORT_COLUMNS = Map.of(
            "invoiceNumber", "invoice_number",
            "status", "status",
            "createdAt", "created_at",
            "updatedAt", "updated_at"
    );

    private static final Set<String> INVOICE_ALLOWED_SORT = INVOICE_SORT_COLUMNS.keySet();

    private PageQueryParser() {
    }

    public static PageQuery parseInvoicePageQuery(int page, int size, String sort) {
        String sortField = "createdAt";
        PageQuery.SortDirection direction = PageQuery.SortDirection.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String candidate = parts[0].trim();
            if (INVOICE_ALLOWED_SORT.contains(candidate)) {
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

    public static String invoiceSqlOrderColumn(String sortField) {
        return INVOICE_SORT_COLUMNS.getOrDefault(sortField, INVOICE_SORT_COLUMNS.get("createdAt"));
    }
}
