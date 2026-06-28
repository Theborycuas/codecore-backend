package com.codecore.organization.infrastructure.persistence.repository;

import com.codecore.organization.infrastructure.persistence.entity.OfficeEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataOfficeRepository extends ReactiveCrudRepository<OfficeEntity, UUID> {

    Mono<Boolean> existsByOrganizationIdAndCode(UUID organizationId, String code);

    Mono<OfficeEntity> findByOfficeIdAndTenantId(UUID officeId, UUID tenantId);

    Mono<Long> countByOrganizationIdAndStatus(UUID organizationId, String status);
}
