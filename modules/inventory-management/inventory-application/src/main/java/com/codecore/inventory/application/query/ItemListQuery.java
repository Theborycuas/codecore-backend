package com.codecore.inventory.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * List filters for Item administration (PASO 20.5.1).
 */
public record ItemListQuery(
        ItemListFilter status,
        String q,
        String code,
        UUID primaryOrganizationId
) {

    public ItemListQuery {
        status = Objects.requireNonNull(status, "status");
        if (q != null && q.isBlank()) {
            q = null;
        }
        if (code != null && code.isBlank()) {
            code = null;
        }
    }

    public static ItemListQuery of(
            String status,
            String q,
            String code,
            UUID primaryOrganizationId
    ) {
        return new ItemListQuery(
                ItemListFilter.parse(status),
                q,
                code,
                primaryOrganizationId
        );
    }
}
