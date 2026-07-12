package com.codecore.inventory.interfaces.http.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateItemRequest(
        @NotBlank @Size(max = 200) String displayName,
        @Size(max = 64) String code,
        UUID primaryOrganizationId
) {
}
