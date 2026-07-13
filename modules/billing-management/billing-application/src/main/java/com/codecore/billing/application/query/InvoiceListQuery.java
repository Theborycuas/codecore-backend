package com.codecore.billing.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * List filters for Invoice administration (PASO 21.5.1).
 */
public record InvoiceListQuery(
        InvoiceListFilter status,
        String q,
        UUID issuerOrganizationId,
        UUID billToPatientId,
        UUID billToOrganizationId
) {

    public InvoiceListQuery {
        status = Objects.requireNonNull(status, "status");
        if (q != null && q.isBlank()) {
            q = null;
        }
    }

    public static InvoiceListQuery of(
            String status,
            String q,
            UUID issuerOrganizationId,
            UUID billToPatientId,
            UUID billToOrganizationId
    ) {
        return new InvoiceListQuery(
                InvoiceListFilter.parse(status),
                q,
                issuerOrganizationId,
                billToPatientId,
                billToOrganizationId
        );
    }
}
