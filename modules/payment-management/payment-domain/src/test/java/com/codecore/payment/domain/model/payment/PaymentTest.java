package com.codecore.payment.domain.model.payment;

import com.codecore.payment.domain.exception.InvalidPaymentStateException;
import com.codecore.payment.domain.valueobject.InvoiceId;
import com.codecore.payment.domain.valueobject.Money;
import com.codecore.payment.domain.valueobject.PaymentId;
import com.codecore.payment.domain.valueobject.PaymentMethodCode;
import com.codecore.payment.domain.valueobject.PaymentStatus;
import com.codecore.payment.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static final Instant NOW = Instant.parse("2026-07-12T18:00:00Z");

    @Test
    void shouldCreateRecordedPaymentWithoutPaymentMethod() {
        PaymentId id = PaymentId.generate();
        TenantId tenantId = TenantId.generate();
        InvoiceId invoiceId = InvoiceId.of(UUID.randomUUID());
        Money amount = Money.of("USD", 15000);

        Payment payment = Payment.create(id, tenantId, invoiceId, amount, null, NOW);

        assertThat(payment.id()).isEqualTo(id);
        assertThat(payment.tenantId()).isEqualTo(tenantId);
        assertThat(payment.invoiceId()).isEqualTo(invoiceId);
        assertThat(payment.amount()).isEqualTo(amount);
        assertThat(payment.paymentMethodCode()).isEmpty();
        assertThat(payment.recordedAt()).isEqualTo(NOW);
        assertThat(payment.status()).isEqualTo(PaymentStatus.RECORDED);
        assertThat(payment.createdAt()).isEqualTo(NOW);
        assertThat(payment.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldCreateRecordedPaymentWithPaymentMethod() {
        PaymentMethodCode methodCode = PaymentMethodCode.of("CARD");

        Payment payment = Payment.create(
                PaymentId.generate(),
                TenantId.generate(),
                InvoiceId.of(UUID.randomUUID()),
                Money.of("USD", 5000),
                methodCode,
                NOW
        );

        assertThat(payment.paymentMethodCode()).contains(methodCode);
    }

    @Test
    void shouldVoidFromRecorded() {
        Payment payment = recordedPayment();

        payment.voidPayment();

        assertThat(payment.status()).isEqualTo(PaymentStatus.VOIDED);
    }

    @Test
    void shouldRejectVoidWhenAlreadyVoided() {
        Payment payment = recordedPayment();
        payment.voidPayment();

        assertThatThrownBy(payment::voidPayment)
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("already voided");
    }

    @Test
    void shouldKeepTenantIdAndInvoiceIdImmutableAfterVoid() {
        Payment payment = recordedPayment();
        TenantId originalTenant = payment.tenantId();
        InvoiceId originalInvoice = payment.invoiceId();

        payment.voidPayment();

        assertThat(payment.tenantId()).isEqualTo(originalTenant);
        assertThat(payment.invoiceId()).isEqualTo(originalInvoice);
    }

    @Test
    void shouldTouchUpdatedAtOnVoid() {
        Payment payment = recordedPayment();
        Instant original = payment.updatedAt();

        payment.voidPayment();

        assertThat(payment.updatedAt()).isAfterOrEqualTo(original);
    }

    @Test
    void shouldReconstitutePayment() {
        PaymentId id = PaymentId.generate();
        TenantId tenantId = TenantId.generate();
        InvoiceId invoiceId = InvoiceId.of(UUID.randomUUID());
        Money amount = Money.of("USD", 999);
        Instant createdAt = NOW.minusSeconds(3600);
        Instant updatedAt = NOW.minusSeconds(60);

        Payment payment = Payment.reconstitute(
                id,
                tenantId,
                invoiceId,
                amount,
                PaymentMethodCode.of("WIRE_TRANSFER"),
                NOW.minusSeconds(3600),
                PaymentStatus.VOIDED,
                createdAt,
                updatedAt
        );

        assertThat(payment.id()).isEqualTo(id);
        assertThat(payment.status()).isEqualTo(PaymentStatus.VOIDED);
        assertThat(payment.paymentMethodCode()).contains(PaymentMethodCode.of("WIRE_TRANSFER"));
        assertThat(payment.createdAt()).isEqualTo(createdAt);
        assertThat(payment.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldNeverExposeRefundLedgerPspOrInvoiceMutationConcernsInPublicApi() {
        assertThat(Payment.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain(
                        "addRefund",
                        "refund",
                        "postToLedger",
                        "post",
                        "toJournalEntry",
                        "capturePsp",
                        "capture",
                        "markInvoicePaid",
                        "payInvoice",
                        "updateContent",
                        "update"
                );
    }

    private static Payment recordedPayment() {
        return Payment.create(
                PaymentId.generate(),
                TenantId.generate(),
                InvoiceId.of(UUID.randomUUID()),
                Money.of("USD", 15000),
                null,
                NOW
        );
    }
}
