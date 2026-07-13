package com.codecore.access.interfaces.http.publicapi.dto;

import com.codecore.access.application.dto.AcceptInvitationResult;
import com.codecore.access.interfaces.http.admin.dto.InvitationResponse;

import java.util.UUID;

public record AcceptInvitationResponse(
        InvitationResponse invitation,
        UUID membershipId
) {

    public static AcceptInvitationResponse from(AcceptInvitationResult result) {
        return new AcceptInvitationResponse(
                InvitationResponse.from(result.invitation()),
                result.membershipId()
        );
    }
}
