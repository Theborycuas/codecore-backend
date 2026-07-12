package com.codecore.inventory.interfaces.http.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateItemRequest(
        @NotBlank @Size(max = 200) String displayName,
        @Size(max = 64) String code,
        UUID primaryOrganizationId
) {
}
