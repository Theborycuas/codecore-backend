package com.codecore.inventory.application.command;

import com.codecore.inventory.domain.valueobject.ItemId;

import java.util.UUID;

/**
 * Full replace of mutable catalog fields (PUT semantics).
 * {@code primaryOrganizationId == null} clears the optional primary organization.
 * {@code code == null} (or blank) clears the optional code.
 */
public record UpdateItemCommand(
        ItemId itemId,
        String displayName,
        String code,
        UUID primaryOrganizationId
) {
}
