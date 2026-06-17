package com.codecore.iam.application.query;

import java.util.Map;
import java.util.Set;

public final class PageQueryParser {

    private static final Map<String, String> USER_SORT_COLUMNS = Map.of(
            "email", "u.normalized_email",
            "status", "u.status",
            "createdAt", "u.created_at",
            "lastLoginAt", "u.last_login_at"
    );

    private static final Set<String> ALLOWED_SORT = USER_SORT_COLUMNS.keySet();

    private PageQueryParser() {
    }

    public static PageQuery parseUserPageQuery(int page, int size, String sort) {
        String sortField = "createdAt";
        PageQuery.SortDirection direction = PageQuery.SortDirection.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String candidate = parts[0].trim();
            if (ALLOWED_SORT.contains(candidate)) {
                sortField = candidate;
            }
            if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = PageQuery.SortDirection.ASC;
            }
        }
        int safeSize = Math.min(Math.max(size, 1), PageQuery.MAX_SIZE);
        int safePage = Math.max(page, 0);
        return new PageQuery(safePage, safeSize, sortField, direction);
    }

    public static String sqlOrderColumn(String sortField) {
        return USER_SORT_COLUMNS.getOrDefault(sortField, USER_SORT_COLUMNS.get("createdAt"));
    }

    private static final Map<String, String> MEMBERSHIP_SORT_COLUMNS = Map.of(
            "status", "m.status",
            "createdAt", "m.created_at"
    );

    private static final Set<String> MEMBERSHIP_ALLOWED_SORT = MEMBERSHIP_SORT_COLUMNS.keySet();

    public static PageQuery parseMembershipPageQuery(int page, int size, String sort) {
        String sortField = "createdAt";
        PageQuery.SortDirection direction = PageQuery.SortDirection.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String candidate = parts[0].trim();
            if (MEMBERSHIP_ALLOWED_SORT.contains(candidate)) {
                sortField = candidate;
            }
            if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = PageQuery.SortDirection.ASC;
            }
        }
        int safeSize = Math.min(Math.max(size, 1), PageQuery.MAX_SIZE);
        int safePage = Math.max(page, 0);
        return new PageQuery(safePage, safeSize, sortField, direction);
    }

    public static String membershipSqlOrderColumn(String sortField) {
        return MEMBERSHIP_SORT_COLUMNS.getOrDefault(sortField, MEMBERSHIP_SORT_COLUMNS.get("createdAt"));
    }

    private static final Map<String, String> ROLE_SORT_COLUMNS = Map.of(
            "code", "code",
            "name", "name",
            "status", "status",
            "createdAt", "created_at"
    );

    private static final Set<String> ROLE_ALLOWED_SORT = ROLE_SORT_COLUMNS.keySet();

    public static PageQuery parseRolePageQuery(int page, int size, String sort) {
        String sortField = "createdAt";
        PageQuery.SortDirection direction = PageQuery.SortDirection.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", 2);
            String candidate = parts[0].trim();
            if (ROLE_ALLOWED_SORT.contains(candidate)) {
                sortField = candidate;
            }
            if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
                direction = PageQuery.SortDirection.ASC;
            }
        }
        int safeSize = Math.min(Math.max(size, 1), PageQuery.MAX_SIZE);
        int safePage = Math.max(page, 0);
        return new PageQuery(safePage, safeSize, sortField, direction);
    }

    public static String roleSqlOrderColumn(String sortField) {
        return ROLE_SORT_COLUMNS.getOrDefault(sortField, ROLE_SORT_COLUMNS.get("createdAt"));
    }
}
