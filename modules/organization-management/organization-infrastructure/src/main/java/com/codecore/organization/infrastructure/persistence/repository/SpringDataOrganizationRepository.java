package com.codecore.organization.infrastructure.persistence.repository;

import com.codecore.organization.infrastructure.persistence.entity.OrganizationEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataOrganizationRepository extends ReactiveCrudRepository<OrganizationEntity, UUID> {

    Mono<Boolean> existsByTenantIdAndCode(UUID tenantId, String code);

    Mono<OrganizationEntity> findByOrganizationIdAndTenantId(UUID organizationId, UUID tenantId);

    Flux<OrganizationEntity> findAllByTenantId(UUID tenantId);

    Mono<Long> countByTenantId(UUID tenantId);
}
