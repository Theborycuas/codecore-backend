package com.codecore.payment.infrastructure.persistence.mapper;

import com.codecore.payment.domain.model.payment.Payment;
import com.codecore.payment.domain.valueobject.InvoiceId;
import com.codecore.payment.domain.valueobject.Money;
import com.codecore.payment.domain.valueobject.PaymentId;
import com.codecore.payment.domain.valueobject.PaymentMethodCode;
import com.codecore.payment.domain.valueobject.PaymentStatus;
import com.codecore.payment.domain.valueobject.TenantId;
import com.codecore.payment.infrastructure.persistence.entity.PaymentEntity;

/**
 * Isomorphic mapping between {@link PaymentEntity} and the {@link Payment} aggregate (ADR-018).
 */
public final class PaymentMapper {

    public Payment toDomain(PaymentEntity entity) {
        return Payment.reconstitute(
                new PaymentId(entity.getPaymentId()),
                new TenantId(entity.getTenantId()),
                new InvoiceId(entity.getInvoiceId()),
                Money.of(entity.getCurrency(), entity.getAmountMinor()),
                entity.getPaymentMethodCode() == null ? null : PaymentMethodCode.of(entity.getPaymentMethodCode()),
                entity.getRecordedAt(),
                PaymentStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PaymentEntity toEntity(Payment payment, boolean isNew) {
        PaymentEntity entity = new PaymentEntity();
        entity.setNewEntity(isNew);
        entity.setPaymentId(payment.id().value());
        entity.setTenantId(payment.tenantId().value());
        entity.setInvoiceId(payment.invoiceId().value());
        entity.setCurrency(payment.amount().currency());
        entity.setAmountMinor(payment.amount().amountMinor());
        entity.setPaymentMethodCode(payment.paymentMethodCode().map(PaymentMethodCode::value).orElse(null));
        entity.setRecordedAt(payment.recordedAt());
        entity.setStatus(payment.status().name());
        entity.setCreatedAt(payment.createdAt());
        entity.setUpdatedAt(payment.updatedAt());
        return entity;
    }
}
