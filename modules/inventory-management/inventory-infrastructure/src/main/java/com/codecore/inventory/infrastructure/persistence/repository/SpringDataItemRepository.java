package com.codecore.inventory.infrastructure.persistence.repository;

import com.codecore.inventory.infrastructure.persistence.entity.ItemEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataItemRepository extends ReactiveCrudRepository<ItemEntity, UUID> {

    Mono<ItemEntity> findByItemIdAndTenantId(UUID itemId, UUID tenantId);

    Mono<Boolean> existsByItemIdAndTenantId(UUID itemId, UUID tenantId);

    Flux<ItemEntity> findAllByTenantId(UUID tenantId);

    Flux<ItemEntity> findAllByTenantIdAndStatus(UUID tenantId, String status);

    Flux<ItemEntity> findAllByTenantIdAndPrimaryOrganizationId(UUID tenantId, UUID primaryOrganizationId);

    Mono<Boolean> existsByTenantIdAndCode(UUID tenantId, String code);

    Mono<Boolean> existsByTenantIdAndCodeAndItemIdNot(UUID tenantId, String code, UUID itemId);

    Mono<Long> countByTenantId(UUID tenantId);
}
