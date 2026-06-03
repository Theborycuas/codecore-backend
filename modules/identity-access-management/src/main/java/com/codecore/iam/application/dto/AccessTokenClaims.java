package com.codecore.iam.application.dto;

/**
 * Application-level claims input for access token issuance (no JWT types).
 */
public record AccessTokenClaims(
        String subject,
        String email,
        String status
) {
}
