package com.codecore.inventory.domain.model.item;

import com.codecore.inventory.domain.exception.InvalidItemStateException;
import com.codecore.inventory.domain.valueobject.ItemCode;
import com.codecore.inventory.domain.valueobject.ItemDisplayName;
import com.codecore.inventory.domain.valueobject.ItemId;
import com.codecore.inventory.domain.valueobject.ItemStatus;
import com.codecore.inventory.domain.valueobject.PrimaryOrganizationId;
import com.codecore.inventory.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Item aggregate root — inventoriable identity / stockable catalog entry (ADR-016).
 * <p>
 * <strong>One sentence:</strong> the inventoriable identity of something that can be stocked,
 * moved, or consumed within a Tenant.
 * <p>
 * Intentionally small: display name, optional code, optional primary organization reference,
 * and soft lifecycle. Never embeds stock quantities, movements, prices, BOM, lots, offices,
 * or clinical references.
 */
public final class Item {

    private final ItemId id;
    private final TenantId tenantId;
    private ItemDisplayName displayName;
    private ItemCode code;
    private PrimaryOrganizationId primaryOrganizationId;
    private ItemStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Item(
            ItemId id,
            TenantId tenantId,
            ItemDisplayName displayName,
            ItemCode code,
            PrimaryOrganizationId primaryOrganizationId,
            ItemStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.code = code;
        this.primaryOrganizationId = primaryOrganizationId;
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Item create(
            ItemId id,
            TenantId tenantId,
            ItemDisplayName displayName,
            Instant now
    ) {
        return create(id, tenantId, displayName, null, null, now);
    }

    public static Item create(
            ItemId id,
            TenantId tenantId,
            ItemDisplayName displayName,
            ItemCode code,
            PrimaryOrganizationId primaryOrganizationId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new Item(
                id,
                tenantId,
                displayName,
                code,
                primaryOrganizationId,
                ItemStatus.ACTIVE,
                now,
                now
        );
    }

    public static Item reconstitute(
            ItemId id,
            TenantId tenantId,
            ItemDisplayName displayName,
            ItemCode code,
            PrimaryOrganizationId primaryOrganizationId,
            ItemStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Item(
                id,
                tenantId,
                displayName,
                code,
                primaryOrganizationId,
                status,
                createdAt,
                updatedAt
        );
    }

    public ItemId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public ItemDisplayName displayName() {
        return displayName;
    }

    public Optional<ItemCode> code() {
        return Optional.ofNullable(code);
    }

    public Optional<PrimaryOrganizationId> primaryOrganizationId() {
        return Optional.ofNullable(primaryOrganizationId);
    }

    public ItemStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void rename(ItemDisplayName newDisplayName) {
        requireActive("rename");
        this.displayName = Objects.requireNonNull(newDisplayName, "newDisplayName");
        touch();
    }

    public void assignCode(ItemCode newCode) {
        requireActive("assign code");
        this.code = Objects.requireNonNull(newCode, "newCode");
        touch();
    }

    public void clearCode() {
        requireActive("clear code");
        this.code = null;
        touch();
    }

    public void assignPrimaryOrganization(PrimaryOrganizationId organizationId) {
        requireActive("assign primary organization");
        this.primaryOrganizationId = Objects.requireNonNull(organizationId, "organizationId");
        touch();
    }

    public void removePrimaryOrganization() {
        requireActive("remove primary organization");
        this.primaryOrganizationId = null;
        touch();
    }

    public void archive() {
        if (status == ItemStatus.ARCHIVED) {
            throw new InvalidItemStateException("Item is already archived");
        }
        this.status = ItemStatus.ARCHIVED;
        touch();
    }

    public void activate() {
        if (status == ItemStatus.ACTIVE) {
            throw new InvalidItemStateException("Item is already active");
        }
        this.status = ItemStatus.ACTIVE;
        touch();
    }

    private void requireActive(String action) {
        if (status != ItemStatus.ACTIVE) {
            throw new InvalidItemStateException("Cannot " + action + " when item is archived");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
