package com.codecore.appointment.application.query;

import java.util.Map;
import java.util.Set;

public final class PageQueryParser {

    private static final Map<String, String> APPOINTMENT_SORT_COLUMNS = Map.of(
            "startsAt", "starts_at",
            "endsAt", "ends_at",
            "status", "status",
            "createdAt", "created_at",
            "updatedAt", "updated_at"
    );

    private static final Set<String> APPOINTMENT_ALLOWED_SORT = APPOINTMENT_SORT_COLUMNS.keySet();

    private PageQueryParser() {
    }

    public static PageQuery parseAppointmentPageQuery(int page, int size, String sort) {
        String sortField = "startsAt";
        PageQuery.SortDirection direction = PageQuery.SortDirection.ASC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String candidate = parts[0].trim();
            if (APPOINTMENT_ALLOWED_SORT.contains(candidate)) {
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

    public static String appointmentSqlOrderColumn(String sortField) {
        return APPOINTMENT_SORT_COLUMNS.getOrDefault(sortField, APPOINTMENT_SORT_COLUMNS.get("startsAt"));
    }
}
