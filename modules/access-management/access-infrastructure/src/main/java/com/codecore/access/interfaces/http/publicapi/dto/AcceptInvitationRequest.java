package com.codecore.access.interfaces.http.publicapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @NotBlank @Size(max = 256) String token,
        @Size(max = 128) String password
) {
}
