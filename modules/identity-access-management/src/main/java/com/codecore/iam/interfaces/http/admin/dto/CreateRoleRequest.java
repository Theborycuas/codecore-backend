package com.codecore.iam.interfaces.http.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoleRequest(
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 200) String name
) {
}
