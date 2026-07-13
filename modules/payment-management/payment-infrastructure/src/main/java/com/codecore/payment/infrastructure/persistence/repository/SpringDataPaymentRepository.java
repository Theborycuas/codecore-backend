package com.codecore.payment.infrastructure.persistence.repository;

import com.codecore.payment.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataPaymentRepository extends ReactiveCrudRepository<PaymentEntity, UUID> {

    Mono<PaymentEntity> findByPaymentIdAndTenantId(UUID paymentId, UUID tenantId);

    Mono<Boolean> existsByPaymentIdAndTenantId(UUID paymentId, UUID tenantId);

    Flux<PaymentEntity> findAllByTenantId(UUID tenantId);

    Flux<PaymentEntity> findAllByTenantIdAndStatus(UUID tenantId, String status);

    Mono<Long> countByTenantId(UUID tenantId);
}
