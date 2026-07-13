package com.codecore.access.application.command;

import java.util.UUID;

public record CreateInvitationCommand(
        String email,
        String roleCode,
        UUID invitedByMembershipId
) {
}
