package com.codecore.iam.interfaces.http.dto;

import java.util.UUID;

/**
 * HTTP response after successful tenant creation.
 */
public record CreateTenantHttpResponse(
        UUID tenantId,
        String name,
        String status
) {
}
