package com.codecore.iam.interfaces.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * HTTP request to register an identity (no profile fields — see user-management).
 */
public record RegisterIdentityRequest(
        @NotNull UUID tenantId,
        @NotBlank String email,
        @NotBlank String password
) {
}
