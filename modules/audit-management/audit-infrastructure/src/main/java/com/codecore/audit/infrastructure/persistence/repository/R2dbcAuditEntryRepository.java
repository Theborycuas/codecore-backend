package com.codecore.audit.infrastructure.persistence.repository;

import com.codecore.audit.application.port.out.AuditEntryQueryPort;
import com.codecore.audit.application.port.out.AuditEntryRepository;
import com.codecore.audit.domain.model.auditentry.AuditEntry;
import com.codecore.audit.domain.valueobject.AuditEntryId;
import com.codecore.audit.domain.valueobject.TenantId;
import com.codecore.audit.infrastructure.persistence.mapper.AuditEntryMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Hexagonal adapter: implements outbound AuditEntry persistence ports using R2DBC (ADR-020).
 */
@Repository
public class R2dbcAuditEntryRepository implements AuditEntryRepository, AuditEntryQueryPort {

    private final SpringDataAuditEntryRepository springDataAuditEntryRepository;
    private final AuditEntryMapper auditEntryMapper;

    public R2dbcAuditEntryRepository(
            SpringDataAuditEntryRepository springDataAuditEntryRepository,
            AuditEntryMapper auditEntryMapper
    ) {
        this.springDataAuditEntryRepository = springDataAuditEntryRepository;
        this.auditEntryMapper = auditEntryMapper;
    }

    @Override
    public Mono<AuditEntry> save(AuditEntry entry) {
        return springDataAuditEntryRepository
                .existsById(entry.id().value())
                .flatMap(exists -> springDataAuditEntryRepository.save(auditEntryMapper.toEntity(entry, !exists)))
                .map(auditEntryMapper::toDomain);
    }

    @Override
    public Mono<AuditEntry> findById(AuditEntryId id) {
        return springDataAuditEntryRepository.findById(id.value())
                .map(auditEntryMapper::toDomain);
    }

    @Override
    public Mono<AuditEntry> findByIdAndTenantId(AuditEntryId id, TenantId tenantId) {
        return springDataAuditEntryRepository.findByAuditEntryIdAndTenantId(id.value(), tenantId.value())
                .map(auditEntryMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsById(AuditEntryId id) {
        return springDataAuditEntryRepository.existsById(id.value());
    }

    @Override
    public Mono<Boolean> existsByIdAndTenantId(AuditEntryId id, TenantId tenantId) {
        return springDataAuditEntryRepository.existsByAuditEntryIdAndTenantId(id.value(), tenantId.value());
    }

    @Override
    public Flux<AuditEntry> findByTenantId(TenantId tenantId) {
        return springDataAuditEntryRepository.findAllByTenantId(tenantId.value())
                .map(auditEntryMapper::toDomain);
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId) {
        return springDataAuditEntryRepository.countByTenantId(tenantId.value());
    }
}
