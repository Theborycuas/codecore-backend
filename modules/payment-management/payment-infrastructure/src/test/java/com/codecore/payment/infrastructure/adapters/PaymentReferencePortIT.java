package com.codecore.payment.infrastructure.adapters;

import com.codecore.payment.application.port.out.PaymentRepository;
import com.codecore.payment.contract.reference.PaymentReferencePort;
import com.codecore.payment.domain.model.payment.Payment;
import com.codecore.payment.domain.valueobject.InvoiceId;
import com.codecore.payment.domain.valueobject.Money;
import com.codecore.payment.domain.valueobject.PaymentId;
import com.codecore.payment.domain.valueobject.TenantId;
import com.codecore.payment.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.payment.testsupport.PaymentPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

@DataR2dbcTest
@Import({
        PaymentPersistenceTestConfiguration.class,
        R2dbcPaymentReferenceAdapter.class
})
class PaymentReferencePortIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-12T21:00:00Z");

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentReferencePort paymentReferencePort;

    @Test
    void shouldReturnTrueForRecordedPaymentInTenant() {
        PaymentId paymentId = PaymentId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(paymentRepository.save(recordedPayment(paymentId, tenantId)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(paymentReferencePort.existsRecordedByIdAndTenant(paymentId, tenantId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseForWrongTenantOrUnknownId() {
        PaymentId paymentId = PaymentId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(paymentRepository.save(recordedPayment(paymentId, tenantId)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(paymentReferencePort.existsRecordedByIdAndTenant(paymentId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(paymentReferencePort.existsRecordedByIdAndTenant(PaymentId.generate(), tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenPaymentIsVoided() {
        PaymentId paymentId = PaymentId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(paymentRepository.save(recordedPayment(paymentId, tenantId)).flatMap(saved -> {
                    saved.voidPayment();
                    return paymentRepository.save(saved);
                }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(paymentReferencePort.existsRecordedByIdAndTenant(paymentId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    private static Payment recordedPayment(PaymentId paymentId, TenantId tenantId) {
        return Payment.create(
                paymentId,
                tenantId,
                InvoiceId.of(UUID.randomUUID()),
                Money.of("USD", 10_000L),
                null,
                NOW
        );
    }
}
