package com.codecore.payment.infrastructure.persistence.repository;

import com.codecore.payment.application.port.out.PaymentQueryPort;
import com.codecore.payment.application.port.out.PaymentRepository;
import com.codecore.payment.domain.model.payment.Payment;
import com.codecore.payment.domain.valueobject.InvoiceId;
import com.codecore.payment.domain.valueobject.Money;
import com.codecore.payment.domain.valueobject.PaymentId;
import com.codecore.payment.domain.valueobject.PaymentMethodCode;
import com.codecore.payment.domain.valueobject.PaymentStatus;
import com.codecore.payment.domain.valueobject.TenantId;
import com.codecore.payment.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.payment.testsupport.PaymentPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(PaymentPersistenceTestConfiguration.class)
class R2dbcPaymentRepositoryIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-12T19:00:00Z");

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentQueryPort paymentQueryPort;

    @Test
    void shouldPersistAndFindById() {
        PaymentId paymentId = PaymentId.generate();
        TenantId tenantId = TenantId.generate();
        Payment payment = recordedPayment(paymentId, tenantId, InvoiceId.of(java.util.UUID.randomUUID()));

        StepVerifier.create(paymentRepository.save(payment))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(paymentId);
                    assertThat(saved.tenantId()).isEqualTo(tenantId);
                    assertThat(saved.status()).isEqualTo(PaymentStatus.RECORDED);
                    assertThat(saved.amount().amountMinor()).isEqualTo(15000);
                })
                .verifyComplete();

        StepVerifier.create(paymentRepository.findById(paymentId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(paymentId))
                .verifyComplete();
    }

    @Test
    void shouldPersistVoidLifecycle() {
        PaymentId paymentId = PaymentId.generate();
        TenantId tenantId = TenantId.generate();
        Payment payment = recordedPayment(paymentId, tenantId, InvoiceId.of(java.util.UUID.randomUUID()));

        StepVerifier.create(paymentRepository.save(payment)
                        .flatMap(saved -> {
                            saved.voidPayment();
                            return paymentRepository.save(saved);
                        }))
                .assertNext(voided -> assertThat(voided.status()).isEqualTo(PaymentStatus.VOIDED))
                .verifyComplete();

        StepVerifier.create(paymentRepository.findById(paymentId))
                .assertNext(found -> assertThat(found.status()).isEqualTo(PaymentStatus.VOIDED))
                .verifyComplete();
    }

    @Test
    void shouldReportExistsByIdAndTenant() {
        PaymentId paymentId = PaymentId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(paymentRepository.save(recordedPayment(
                        paymentId, tenantId, InvoiceId.of(java.util.UUID.randomUUID()))))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(paymentRepository.existsById(paymentId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(paymentRepository.existsByIdAndTenantId(paymentId, tenantId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(paymentRepository.existsByIdAndTenantId(paymentId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldIsolateCrossTenantReads() {
        PaymentId paymentId = PaymentId.generate();
        TenantId tenantId = TenantId.generate();
        TenantId otherTenantId = TenantId.generate();

        StepVerifier.create(paymentRepository.save(recordedPayment(
                        paymentId, tenantId, InvoiceId.of(java.util.UUID.randomUUID()))))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(paymentQueryPort.findByIdAndTenantId(paymentId, tenantId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(paymentId))
                .verifyComplete();

        StepVerifier.create(paymentQueryPort.findByIdAndTenantId(paymentId, otherTenantId))
                .verifyComplete();
    }

    @Test
    void shouldCountAndFindByTenantIdAndStatus() {
        TenantId tenantId = TenantId.generate();
        PaymentId recordedId = PaymentId.generate();
        PaymentId voidedId = PaymentId.generate();

        StepVerifier.create(paymentRepository.save(recordedPayment(
                        recordedId, tenantId, InvoiceId.of(java.util.UUID.randomUUID()))))
                .expectNextCount(1)
                .verifyComplete();

        Payment toVoid = recordedPayment(voidedId, tenantId, InvoiceId.of(java.util.UUID.randomUUID()));
        toVoid.voidPayment();
        StepVerifier.create(paymentRepository.save(toVoid))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(paymentQueryPort.countByTenantId(tenantId))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(paymentQueryPort.findByTenantIdAndStatus(tenantId, PaymentStatus.RECORDED))
                .assertNext(found -> assertThat(found.id()).isEqualTo(recordedId))
                .verifyComplete();

        StepVerifier.create(paymentQueryPort.findByTenantIdAndStatus(tenantId, PaymentStatus.VOIDED))
                .assertNext(found -> assertThat(found.id()).isEqualTo(voidedId))
                .verifyComplete();
    }

    @Test
    void shouldPersistOptionalPaymentMethodCode() {
        PaymentId paymentId = PaymentId.generate();
        TenantId tenantId = TenantId.generate();
        Payment payment = Payment.create(
                paymentId,
                tenantId,
                InvoiceId.of(java.util.UUID.randomUUID()),
                Money.of("USD", 5000),
                PaymentMethodCode.of("CASH"),
                NOW
        );

        StepVerifier.create(paymentRepository.save(payment))
                .assertNext(saved -> assertThat(saved.paymentMethodCode())
                        .isPresent()
                        .get()
                        .isEqualTo(PaymentMethodCode.of("CASH")))
                .verifyComplete();

        StepVerifier.create(paymentRepository.findById(paymentId))
                .assertNext(found -> assertThat(found.paymentMethodCode()).isPresent())
                .verifyComplete();
    }

    private static Payment recordedPayment(PaymentId paymentId, TenantId tenantId, InvoiceId invoiceId) {
        return Payment.create(
                paymentId,
                tenantId,
                invoiceId,
                Money.of("USD", 15000),
                null,
                NOW
        );
    }
}
