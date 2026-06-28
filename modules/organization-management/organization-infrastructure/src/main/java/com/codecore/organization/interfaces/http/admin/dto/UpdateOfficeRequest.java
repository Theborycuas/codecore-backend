package com.codecore.organization.interfaces.http.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOfficeRequest(
        @NotBlank @Size(max = 200) String name
) {
}
