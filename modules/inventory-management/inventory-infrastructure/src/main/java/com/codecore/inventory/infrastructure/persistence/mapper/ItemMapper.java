package com.codecore.inventory.infrastructure.persistence.mapper;

import com.codecore.inventory.domain.model.item.Item;
import com.codecore.inventory.domain.valueobject.ItemCode;
import com.codecore.inventory.domain.valueobject.ItemDisplayName;
import com.codecore.inventory.domain.valueobject.ItemId;
import com.codecore.inventory.domain.valueobject.ItemStatus;
import com.codecore.inventory.domain.valueobject.PrimaryOrganizationId;
import com.codecore.inventory.domain.valueobject.TenantId;
import com.codecore.inventory.infrastructure.persistence.entity.ItemEntity;

/**
 * Isomorphic mapping between {@link ItemEntity} and {@link Item}.
 */
public final class ItemMapper {

    public Item toDomain(ItemEntity entity) {
        ItemCode code = entity.getCode() == null ? null : ItemCode.of(entity.getCode());
        PrimaryOrganizationId primaryOrganizationId = entity.getPrimaryOrganizationId() == null
                ? null
                : PrimaryOrganizationId.of(entity.getPrimaryOrganizationId());

        return Item.reconstitute(
                new ItemId(entity.getItemId()),
                new TenantId(entity.getTenantId()),
                ItemDisplayName.of(entity.getDisplayName()),
                code,
                primaryOrganizationId,
                ItemStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ItemEntity toEntity(Item item, boolean isNew) {
        ItemEntity entity = new ItemEntity();
        entity.setNewEntity(isNew);
        entity.setItemId(item.id().value());
        entity.setTenantId(item.tenantId().value());
        entity.setPrimaryOrganizationId(
                item.primaryOrganizationId().map(PrimaryOrganizationId::value).orElse(null)
        );
        entity.setDisplayName(item.displayName().value());
        entity.setCode(item.code().map(ItemCode::value).orElse(null));
        entity.setStatus(item.status().name());
        entity.setCreatedAt(item.createdAt());
        entity.setUpdatedAt(item.updatedAt());
        return entity;
    }
}
