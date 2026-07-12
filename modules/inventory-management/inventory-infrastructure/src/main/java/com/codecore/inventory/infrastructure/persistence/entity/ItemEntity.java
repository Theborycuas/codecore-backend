package com.codecore.inventory.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC row mapping for {@code inventory.item}.
 */
@Table(name = "item", schema = "inventory")
public class ItemEntity implements Persistable<UUID> {

    @Transient
    private boolean newEntity;

    @Id
    @Column("item_id")
    private UUID itemId;

    @Column("tenant_id")
    private UUID tenantId;

    @Column("primary_organization_id")
    private UUID primaryOrganizationId;

    @Column("display_name")
    private String displayName;

    private String code;

    private String status;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    public ItemEntity() {
    }

    @Override
    public UUID getId() {
        return itemId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public void setNewEntity(boolean newEntity) {
        this.newEntity = newEntity;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getPrimaryOrganizationId() {
        return primaryOrganizationId;
    }

    public void setPrimaryOrganizationId(UUID primaryOrganizationId) {
        this.primaryOrganizationId = primaryOrganizationId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
