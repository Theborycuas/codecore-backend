package com.codecore.access.interfaces.http.admin.dto;

import com.codecore.access.application.dto.CreateInvitationResult;

/**
 * Create response includes the raw token once (never persisted; never returned on subsequent reads).
 */
public record InvitationCreatedResponse(
        InvitationResponse invitation,
        String token
) {

    public static InvitationCreatedResponse from(CreateInvitationResult result) {
        return new InvitationCreatedResponse(
                InvitationResponse.from(result.invitation()),
                result.rawToken()
        );
    }
}
