package com.codecore.billing.infrastructure.persistence.repository;

import com.codecore.billing.infrastructure.persistence.entity.InvoiceLineEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataInvoiceLineRepository extends ReactiveCrudRepository<InvoiceLineEntity, UUID> {

    Flux<InvoiceLineEntity> findAllByInvoiceId(UUID invoiceId);

    Mono<Void> deleteAllByInvoiceId(UUID invoiceId);
}
