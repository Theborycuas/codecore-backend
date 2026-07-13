package com.codecore.access.application.dto;

import java.util.UUID;

public record AcceptInvitationResult(
        AdminInvitationView invitation,
        UUID membershipId
) {
}
