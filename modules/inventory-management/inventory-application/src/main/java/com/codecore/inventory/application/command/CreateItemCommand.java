package com.codecore.inventory.application.command;

import java.util.UUID;

public record CreateItemCommand(
        String displayName,
        String code,
        UUID primaryOrganizationId
) {
}
