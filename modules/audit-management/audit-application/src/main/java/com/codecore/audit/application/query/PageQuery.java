package com.codecore.audit.application.query;

import java.util.Objects;

public record PageQuery(
        int page,
        int size,
        String sortField,
        SortDirection sortDirection
) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
        sortField = Objects.requireNonNull(sortField, "sortField");
        sortDirection = Objects.requireNonNull(sortDirection, "sortDirection");
    }

    public long offset() {
        return (long) page * size;
    }

    public enum SortDirection {
        ASC,
        DESC
    }
}
