package com.codecore.organization.application.query;

import java.util.Map;
import java.util.Set;

public final class PageQueryParser {

    private static final Map<String, String> ORGANIZATION_SORT_COLUMNS = Map.of(
            "code", "code",
            "name", "name",
            "status", "status",
            "createdAt", "created_at",
            "updatedAt", "updated_at"
    );

    private static final Set<String> ORGANIZATION_ALLOWED_SORT = ORGANIZATION_SORT_COLUMNS.keySet();

    private static final Map<String, String> OFFICE_SORT_COLUMNS = Map.of(
            "code", "code",
            "name", "name",
            "status", "status",
            "createdAt", "created_at",
            "updatedAt", "updated_at"
    );

    private static final Set<String> OFFICE_ALLOWED_SORT = OFFICE_SORT_COLUMNS.keySet();

    private PageQueryParser() {
    }

    public static PageQuery parseOrganizationPageQuery(int page, int size, String sort) {
        return parse(page, size, sort, ORGANIZATION_ALLOWED_SORT, "createdAt", PageQuery.SortDirection.DESC);
    }

    public static String organizationSqlOrderColumn(String sortField) {
        return ORGANIZATION_SORT_COLUMNS.getOrDefault(sortField, ORGANIZATION_SORT_COLUMNS.get("createdAt"));
    }

    public static PageQuery parseOfficePageQuery(int page, int size, String sort) {
        return parse(page, size, sort, OFFICE_ALLOWED_SORT, "createdAt", PageQuery.SortDirection.DESC);
    }

    public static String officeSqlOrderColumn(String sortField) {
        return OFFICE_SORT_COLUMNS.getOrDefault(sortField, OFFICE_SORT_COLUMNS.get("createdAt"));
    }

    private static final Map<String, String> STAFF_ASSIGNMENT_SORT_COLUMNS = Map.of(
            "membershipId", "membership_id",
            "organizationId", "organization_id",
            "officeId", "office_id",
            "createdAt", "created_at",
            "updatedAt", "updated_at"
    );

    private static final Set<String> STAFF_ASSIGNMENT_ALLOWED_SORT = STAFF_ASSIGNMENT_SORT_COLUMNS.keySet();

    public static PageQuery parseStaffAssignmentPageQuery(int page, int size, String sort) {
        return parse(page, size, sort, STAFF_ASSIGNMENT_ALLOWED_SORT, "createdAt", PageQuery.SortDirection.DESC);
    }

    public static String staffAssignmentSqlOrderColumn(String sortField) {
        return STAFF_ASSIGNMENT_SORT_COLUMNS.getOrDefault(
                sortField,
                STAFF_ASSIGNMENT_SORT_COLUMNS.get("createdAt")
        );
    }

    private static PageQuery parse(
            int page,
            int size,
            String sort,
            Set<String> allowedSort,
            String defaultField,
            PageQuery.SortDirection defaultDirection
    ) {
        String sortField = defaultField;
        PageQuery.SortDirection direction = defaultDirection;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String candidate = parts[0].trim();
            if (allowedSort.contains(candidate)) {
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
}
