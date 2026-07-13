package com.codecore.access.application.command;

public record AcceptInvitationCommand(
        String rawToken,
        String password
) {
}
