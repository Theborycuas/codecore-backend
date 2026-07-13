package com.codecore.billing.infrastructure.persistence.repository;

import com.codecore.billing.infrastructure.persistence.entity.InvoiceEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataInvoiceRepository extends ReactiveCrudRepository<InvoiceEntity, UUID> {

    Mono<InvoiceEntity> findByInvoiceIdAndTenantId(UUID invoiceId, UUID tenantId);

    Mono<Boolean> existsByInvoiceIdAndTenantId(UUID invoiceId, UUID tenantId);

    Flux<InvoiceEntity> findAllByTenantId(UUID tenantId);

    Flux<InvoiceEntity> findAllByTenantIdAndStatus(UUID tenantId, String status);

    Mono<Boolean> existsByTenantIdAndInvoiceNumber(UUID tenantId, String invoiceNumber);

    Mono<Boolean> existsByTenantIdAndInvoiceNumberAndInvoiceIdNot(UUID tenantId, String invoiceNumber, UUID invoiceId);

    Mono<Long> countByTenantId(UUID tenantId);
}
