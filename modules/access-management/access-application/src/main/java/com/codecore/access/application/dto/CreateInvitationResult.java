package com.codecore.access.application.dto;

public record CreateInvitationResult(
        AdminInvitationView invitation,
        String rawToken
) {
}
