package com.codecore.iam.interfaces.http.dto;

import java.util.UUID;

/**
 * HTTP response after successful identity registration (no credential material).
 */
public record RegisterIdentityResponse(
        UUID identityId,
        UUID tenantId,
        String email,
        String status
) {
}
