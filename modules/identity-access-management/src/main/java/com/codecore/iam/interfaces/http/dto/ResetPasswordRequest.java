package com.codecore.iam.interfaces.http.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Reset-password request. Token is the raw opaque value; server hashes before use-case call.
 */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank String password,
        UUID tenantId
) {
}
