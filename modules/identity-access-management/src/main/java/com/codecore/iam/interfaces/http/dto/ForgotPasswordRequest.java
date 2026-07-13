package com.codecore.iam.interfaces.http.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Forgot-password request. Always answered with 204 (anti-enumeration).
 */
public record ForgotPasswordRequest(
        @NotBlank String email,
        UUID tenantId
) {
}
