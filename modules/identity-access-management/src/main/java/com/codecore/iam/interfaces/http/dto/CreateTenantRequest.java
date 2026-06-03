package com.codecore.iam.interfaces.http.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * HTTP request to create a tenant.
 */
public record CreateTenantRequest(
        @NotBlank String name
) {
}
