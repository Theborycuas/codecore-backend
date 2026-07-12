package com.codecore.inventory.contract.authorization;

import java.util.Set;

/**
 * Inventory (Item) permission contract (FASE 20.5).
 * <p>
 * String codes are the canonical identifiers seeded in {@code iam.permission}.
 * IAM maps them to {@code PermissionCode} via {@code IamPermissionCatalog}.
 * <p>
 * Intentionally limited to inventoriable-identity lifecycle — no vertical-specific verbs (ADR-016).
 * {@code activate} maps to {@link #ITEM_UPDATE} (mirror Patient activate → update).
 */
public final class ItemPermissionCatalog {

    public static final String ITEM_CREATE = "item:create";
    public static final String ITEM_READ = "item:read";
    public static final String ITEM_UPDATE = "item:update";
    public static final String ITEM_ARCHIVE = "item:archive";

    /** Full Item catalog lifecycle contract (FASE 20 — no stock/price/BOM/clinical). */
    public static final Set<String> ALL = Set.of(
            ITEM_CREATE,
            ITEM_READ,
            ITEM_UPDATE,
            ITEM_ARCHIVE
    );

    /** Read-only consultation of inventoriable catalog identity. */
    public static final Set<String> ITEM_READ_ONLY = Set.of(ITEM_READ);

    private ItemPermissionCatalog() {
    }
}
