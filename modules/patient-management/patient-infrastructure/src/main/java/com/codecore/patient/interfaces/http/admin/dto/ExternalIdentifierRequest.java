package com.codecore.patient.interfaces.http.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExternalIdentifierRequest(
        @NotBlank @Size(max = 64) String type,
        @NotBlank @Size(max = 128) String value
) {
}
