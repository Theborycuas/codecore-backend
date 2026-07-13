package com.codecore.payment.domain.model.payment;

import com.codecore.payment.domain.exception.InvalidPaymentStateException;
import com.codecore.payment.domain.valueobject.InvoiceId;
import com.codecore.payment.domain.valueobject.Money;
import com.codecore.payment.domain.valueobject.PaymentId;
import com.codecore.payment.domain.valueobject.PaymentMethodCode;
import com.codecore.payment.domain.valueobject.PaymentStatus;
import com.codecore.payment.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Payment aggregate root — the settlement record of a payment against an Invoice commercial
 * claim within a Tenant (ADR-018).
 * <p>
 * <strong>One sentence:</strong> the settlement record of an amount paid against an Invoice
 * under a Tenant.
 * <p>
 * Intentionally small: invoice reference + Money + optional opaque payment method + soft
 * lifecycle RECORDED/VOIDED. Never embeds refunds, ledger postings, PSP capture state, or
 * Invoice mutation — it never marks the Invoice as {@code PAID}.
 */
public final class Payment {

    private final PaymentId id;
    private final TenantId tenantId;
    private final InvoiceId invoiceId;
    private final Money amount;
    private final PaymentMethodCode paymentMethodCode;
    private final Instant recordedAt;
    private PaymentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Payment(
            PaymentId id,
            TenantId tenantId,
            InvoiceId invoiceId,
            Money amount,
            PaymentMethodCode paymentMethodCode,
            Instant recordedAt,
            PaymentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.invoiceId = Objects.requireNonNull(invoiceId, "invoiceId");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.paymentMethodCode = paymentMethodCode;
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Payment create(
            PaymentId id,
            TenantId tenantId,
            InvoiceId invoiceId,
            Money amount,
            PaymentMethodCode paymentMethodCode,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new Payment(
                id,
                tenantId,
                invoiceId,
                amount,
                paymentMethodCode,
                now,
                PaymentStatus.RECORDED,
                now,
                now
        );
    }

    public static Payment reconstitute(
            PaymentId id,
            TenantId tenantId,
            InvoiceId invoiceId,
            Money amount,
            PaymentMethodCode paymentMethodCode,
            Instant recordedAt,
            PaymentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Payment(
                id,
                tenantId,
                invoiceId,
                amount,
                paymentMethodCode,
                recordedAt,
                status,
                createdAt,
                updatedAt
        );
    }

    public PaymentId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public InvoiceId invoiceId() {
        return invoiceId;
    }

    public Money amount() {
        return amount;
    }

    public Optional<PaymentMethodCode> paymentMethodCode() {
        return Optional.ofNullable(paymentMethodCode);
    }

    public Instant recordedAt() {
        return recordedAt;
    }

    public PaymentStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    /** {@code RECORDED -> VOIDED}. No physical delete; no un-void; no ReferencePort re-validation. */
    public void voidPayment() {
        if (status == PaymentStatus.VOIDED) {
            throw new InvalidPaymentStateException("Payment is already voided");
        }
        this.status = PaymentStatus.VOIDED;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
