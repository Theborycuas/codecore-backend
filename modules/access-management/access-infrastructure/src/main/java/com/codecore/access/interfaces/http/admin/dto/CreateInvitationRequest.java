package com.codecore.access.interfaces.http.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Invitation create request (ADR-019). No client-supplied {@code tenantId} — the tenant is
 * always resolved from the authenticated JWT context.
 */
public record CreateInvitationRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 64) String roleCode
) {
}
