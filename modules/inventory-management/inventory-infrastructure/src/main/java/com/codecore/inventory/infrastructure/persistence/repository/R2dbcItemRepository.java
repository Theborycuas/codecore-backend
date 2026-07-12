package com.codecore.inventory.infrastructure.persistence.repository;

import com.codecore.inventory.application.port.out.ItemQueryPort;
import com.codecore.inventory.application.port.out.ItemRepository;
import com.codecore.inventory.domain.model.item.Item;
import com.codecore.inventory.domain.valueobject.ItemCode;
import com.codecore.inventory.domain.valueobject.ItemId;
import com.codecore.inventory.domain.valueobject.ItemStatus;
import com.codecore.inventory.domain.valueobject.PrimaryOrganizationId;
import com.codecore.inventory.domain.valueobject.TenantId;
import com.codecore.inventory.infrastructure.persistence.mapper.ItemMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Hexagonal adapter: implements outbound Item persistence ports using R2DBC.
 * No child tables — Item is a single-row aggregate (ADR-016).
 */
@Repository
public class R2dbcItemRepository implements ItemRepository, ItemQueryPort {

    private final SpringDataItemRepository springDataItemRepository;
    private final ItemMapper itemMapper;

    public R2dbcItemRepository(
            SpringDataItemRepository springDataItemRepository,
            ItemMapper itemMapper
    ) {
        this.springDataItemRepository = springDataItemRepository;
        this.itemMapper = itemMapper;
    }

    @Override
    public Mono<Item> save(Item item) {
        return springDataItemRepository
                .existsById(item.id().value())
                .flatMap(exists -> springDataItemRepository.save(itemMapper.toEntity(item, !exists)))
                .map(itemMapper::toDomain);
    }

    @Override
    public Mono<Item> findById(ItemId id) {
        return springDataItemRepository.findById(id.value())
                .map(itemMapper::toDomain);
    }

    @Override
    public Mono<Item> findByIdAndTenantId(ItemId id, TenantId tenantId) {
        return springDataItemRepository.findByItemIdAndTenantId(id.value(), tenantId.value())
                .map(itemMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsById(ItemId id) {
        return springDataItemRepository.existsById(id.value());
    }

    @Override
    public Mono<Boolean> existsByIdAndTenantId(ItemId id, TenantId tenantId) {
        return springDataItemRepository.existsByItemIdAndTenantId(id.value(), tenantId.value());
    }

    @Override
    public Flux<Item> findByTenantId(TenantId tenantId) {
        return springDataItemRepository.findAllByTenantId(tenantId.value())
                .map(itemMapper::toDomain);
    }

    @Override
    public Flux<Item> findByTenantIdAndStatus(TenantId tenantId, ItemStatus status) {
        return springDataItemRepository.findAllByTenantIdAndStatus(tenantId.value(), status.name())
                .map(itemMapper::toDomain);
    }

    @Override
    public Flux<Item> findByTenantIdAndPrimaryOrganizationId(
            TenantId tenantId,
            PrimaryOrganizationId primaryOrganizationId
    ) {
        return springDataItemRepository
                .findAllByTenantIdAndPrimaryOrganizationId(tenantId.value(), primaryOrganizationId.value())
                .map(itemMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByTenantIdAndCode(TenantId tenantId, ItemCode code) {
        return springDataItemRepository.existsByTenantIdAndCode(tenantId.value(), code.value());
    }

    @Override
    public Mono<Boolean> existsByTenantIdAndCodeExcludingId(
            TenantId tenantId,
            ItemCode code,
            ItemId excludeItemId
    ) {
        return springDataItemRepository.existsByTenantIdAndCodeAndItemIdNot(
                tenantId.value(),
                code.value(),
                excludeItemId.value()
        );
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId) {
        return springDataItemRepository.countByTenantId(tenantId.value());
    }
}
