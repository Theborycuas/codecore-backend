package com.codecore.payment.application.admin;

import com.codecore.billing.contract.reference.InvoiceReferencePort;
import com.codecore.payment.application.command.CreatePaymentCommand;
import com.codecore.payment.application.dto.AdminPaymentView;
import com.codecore.payment.application.port.out.PaymentAdminQueryRepository;
import com.codecore.payment.application.port.out.PaymentQueryPort;
import com.codecore.payment.application.port.out.PaymentRepository;
import com.codecore.payment.application.port.out.TenantContextAccessor;
import com.codecore.payment.domain.exception.InvoiceNotFoundException;
import com.codecore.payment.domain.exception.InvalidPaymentStateException;
import com.codecore.payment.domain.exception.PaymentNotFoundException;
import com.codecore.payment.domain.model.payment.Payment;
import com.codecore.payment.domain.valueobject.InvoiceId;
import com.codecore.payment.domain.valueobject.Money;
import com.codecore.payment.domain.valueobject.PaymentId;
import com.codecore.payment.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentAdministrationUseCaseImplTest {

    @Mock
    private TenantContextAccessor tenantContextAccessor;

    @Mock
    private PaymentAdminQueryRepository paymentAdminQueryRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentQueryPort paymentQueryPort;

    @Mock
    private InvoiceReferencePort invoiceReferencePort;

    @Mock
    private TransactionalOperator transactionalOperator;

    private PaymentAdministrationUseCaseImpl useCase;

    private final TenantId tenantId = new TenantId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        useCase = new PaymentAdministrationUseCaseImpl(
                tenantContextAccessor,
                paymentAdminQueryRepository,
                paymentRepository,
                paymentQueryPort,
                invoiceReferencePort,
                transactionalOperator
        );
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreatePaymentWhenInvoiceIsIssued() {
        UUID invoiceUuid = UUID.randomUUID();
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        when(invoiceReferencePort.existsIssuedByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(paymentRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        CreatePaymentCommand command = new CreatePaymentCommand(invoiceUuid, "USD", 1000L, "CASH");

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> {
                    org.assertj.core.api.Assertions.assertThat(view.invoiceUuid()).isEqualTo(invoiceUuid);
                    org.assertj.core.api.Assertions.assertThat(view.currency()).isEqualTo("USD");
                    org.assertj.core.api.Assertions.assertThat(view.amountMinor()).isEqualTo(1000L);
                })
                .verifyComplete();

        verify(paymentRepository).save(any());
    }

    @Test
    void shouldRejectCreateWhenInvoiceNotIssued() {
        UUID invoiceUuid = UUID.randomUUID();
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        when(invoiceReferencePort.existsIssuedByIdAndTenant(any(), any())).thenReturn(Mono.just(false));

        CreatePaymentCommand command = new CreatePaymentCommand(invoiceUuid, "USD", 1000L, null);

        StepVerifier.create(useCase.execute(command))
                .expectError(InvoiceNotFoundException.class)
                .verify();

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void shouldVoidRecordedPaymentWithoutRevalidatingInvoicePort() {
        Payment payment = Payment.create(
                PaymentId.generate(),
                tenantId,
                InvoiceId.of(UUID.randomUUID()),
                Money.of("USD", 500L),
                null,
                Instant.now()
        );
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        when(paymentQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(payment));
        when(paymentRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.voidPayment(payment.id()))
                .assertNext(view -> org.assertj.core.api.Assertions.assertThat(view.status())
                        .isEqualTo(com.codecore.payment.domain.valueobject.PaymentStatus.VOIDED))
                .verifyComplete();

        verifyNoInteractions(invoiceReferencePort);
    }

    @Test
    void shouldFailVoidWhenPaymentNotFoundInTenant() {
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        when(paymentQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.voidPayment(PaymentId.generate()))
                .expectError(PaymentNotFoundException.class)
                .verify();
    }

    @Test
    void shouldFailVoidingAlreadyVoidedPayment() {
        Payment payment = Payment.create(
                PaymentId.generate(),
                tenantId,
                InvoiceId.of(UUID.randomUUID()),
                Money.of("USD", 500L),
                null,
                Instant.now()
        );
        payment.voidPayment();
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        when(paymentQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(payment));

        StepVerifier.create(useCase.voidPayment(payment.id()))
                .expectError(InvalidPaymentStateException.class)
                .verify();
    }
}
