package com.codecore.audit.infrastructure.persistence.repository;

import com.codecore.audit.infrastructure.persistence.entity.AuditEntryEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataAuditEntryRepository extends ReactiveCrudRepository<AuditEntryEntity, UUID> {

    Mono<AuditEntryEntity> findByAuditEntryIdAndTenantId(UUID auditEntryId, UUID tenantId);

    Mono<Boolean> existsByAuditEntryIdAndTenantId(UUID auditEntryId, UUID tenantId);

    Flux<AuditEntryEntity> findAllByTenantId(UUID tenantId);

    Mono<Long> countByTenantId(UUID tenantId);
}
