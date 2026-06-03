package com.codecore.iam.interfaces.http.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * HTTP login request (tenant supplied via {@code X-Tenant-Id} header).
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}
