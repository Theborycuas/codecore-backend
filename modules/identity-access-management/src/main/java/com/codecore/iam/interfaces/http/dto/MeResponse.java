package com.codecore.iam.interfaces.http.dto;

import com.codecore.iam.application.dto.AuthenticatedPrincipal;

/**
 * Current authenticated identity (from JWT context).
 */
public record MeResponse(
        String identityId,
        String email,
        String status
) {

    public static MeResponse from(AuthenticatedPrincipal principal) {
        return new MeResponse(
                principal.identityId().asString(),
                principal.email(),
                principal.status().name()
        );
    }
}
