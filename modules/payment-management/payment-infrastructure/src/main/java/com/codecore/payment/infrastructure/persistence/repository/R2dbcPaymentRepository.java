package com.codecore.payment.infrastructure.persistence.repository;

import com.codecore.payment.application.port.out.PaymentQueryPort;
import com.codecore.payment.application.port.out.PaymentRepository;
import com.codecore.payment.domain.model.payment.Payment;
import com.codecore.payment.domain.valueobject.PaymentId;
import com.codecore.payment.domain.valueobject.PaymentStatus;
import com.codecore.payment.domain.valueobject.TenantId;
import com.codecore.payment.infrastructure.persistence.mapper.PaymentMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Hexagonal adapter: implements outbound Payment persistence ports using R2DBC (ADR-018).
 */
@Repository
public class R2dbcPaymentRepository implements PaymentRepository, PaymentQueryPort {

    private final SpringDataPaymentRepository springDataPaymentRepository;
    private final PaymentMapper paymentMapper;

    public R2dbcPaymentRepository(
            SpringDataPaymentRepository springDataPaymentRepository,
            PaymentMapper paymentMapper
    ) {
        this.springDataPaymentRepository = springDataPaymentRepository;
        this.paymentMapper = paymentMapper;
    }

    @Override
    public Mono<Payment> save(Payment payment) {
        return springDataPaymentRepository
                .existsById(payment.id().value())
                .flatMap(exists -> springDataPaymentRepository.save(paymentMapper.toEntity(payment, !exists)))
                .map(paymentMapper::toDomain);
    }

    @Override
    public Mono<Payment> findById(PaymentId id) {
        return springDataPaymentRepository.findById(id.value())
                .map(paymentMapper::toDomain);
    }

    @Override
    public Mono<Payment> findByIdAndTenantId(PaymentId id, TenantId tenantId) {
        return springDataPaymentRepository.findByPaymentIdAndTenantId(id.value(), tenantId.value())
                .map(paymentMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsById(PaymentId id) {
        return springDataPaymentRepository.existsById(id.value());
    }

    @Override
    public Mono<Boolean> existsByIdAndTenantId(PaymentId id, TenantId tenantId) {
        return springDataPaymentRepository.existsByPaymentIdAndTenantId(id.value(), tenantId.value());
    }

    @Override
    public Flux<Payment> findByTenantId(TenantId tenantId) {
        return springDataPaymentRepository.findAllByTenantId(tenantId.value())
                .map(paymentMapper::toDomain);
    }

    @Override
    public Flux<Payment> findByTenantIdAndStatus(TenantId tenantId, PaymentStatus status) {
        return springDataPaymentRepository.findAllByTenantIdAndStatus(tenantId.value(), status.name())
                .map(paymentMapper::toDomain);
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId) {
        return springDataPaymentRepository.countByTenantId(tenantId.value());
    }
}
