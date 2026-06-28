package com.codecore.organization.interfaces.http.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 200) String name
) {
}
