package com.codecore.payment.application.admin;

import com.codecore.billing.contract.reference.InvoiceReferencePort;
import com.codecore.payment.application.command.CreatePaymentCommand;
import com.codecore.payment.application.dto.AdminPaymentView;
import com.codecore.payment.application.dto.PagedResult;
import com.codecore.payment.application.port.in.CreatePaymentUseCase;
import com.codecore.payment.application.port.in.GetPaymentUseCase;
import com.codecore.payment.application.port.in.ListPaymentsUseCase;
import com.codecore.payment.application.port.in.VoidPaymentUseCase;
import com.codecore.payment.application.port.out.PaymentAdminQueryRepository;
import com.codecore.payment.application.port.out.PaymentQueryPort;
import com.codecore.payment.application.port.out.PaymentRepository;
import com.codecore.payment.application.port.out.TenantContextAccessor;
import com.codecore.payment.application.query.PageQuery;
import com.codecore.payment.application.query.PaymentListQuery;
import com.codecore.payment.domain.exception.InvalidDomainValueException;
import com.codecore.payment.domain.exception.InvoiceNotFoundException;
import com.codecore.payment.domain.exception.PaymentNotFoundException;
import com.codecore.payment.domain.model.payment.Payment;
import com.codecore.payment.domain.valueobject.InvoiceId;
import com.codecore.payment.domain.valueobject.Money;
import com.codecore.payment.domain.valueobject.PaymentId;
import com.codecore.payment.domain.valueobject.PaymentMethodCode;
import com.codecore.payment.domain.valueobject.TenantId;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Payment administration use cases (PASO 22.6) — write-time validation of the referenced
 * Invoice via {@link InvoiceReferencePort} (ADR-013 / ADR-018). {@code void} does not
 * revalidate the port (mirrors Billing {@code voidInvoice} — ADR-017 pattern).
 */
public final class PaymentAdministrationUseCaseImpl
        implements ListPaymentsUseCase,
        GetPaymentUseCase,
        CreatePaymentUseCase,
        VoidPaymentUseCase {

    private final TenantContextAccessor tenantContextAccessor;
    private final PaymentAdminQueryRepository paymentAdminQueryRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentQueryPort paymentQueryPort;
    private final InvoiceReferencePort invoiceReferencePort;
    private final TransactionalOperator transactionalOperator;

    public PaymentAdministrationUseCaseImpl(
            TenantContextAccessor tenantContextAccessor,
            PaymentAdminQueryRepository paymentAdminQueryRepository,
            PaymentRepository paymentRepository,
            PaymentQueryPort paymentQueryPort,
            InvoiceReferencePort invoiceReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        this.tenantContextAccessor = Objects.requireNonNull(tenantContextAccessor, "tenantContextAccessor");
        this.paymentAdminQueryRepository = Objects.requireNonNull(
                paymentAdminQueryRepository,
                "paymentAdminQueryRepository"
        );
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository");
        this.paymentQueryPort = Objects.requireNonNull(paymentQueryPort, "paymentQueryPort");
        this.invoiceReferencePort = Objects.requireNonNull(invoiceReferencePort, "invoiceReferencePort");
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<PagedResult<AdminPaymentView>> execute(PaymentListQuery filter, PageQuery pageQuery) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> paymentAdminQueryRepository.countByTenantId(tenantId, filter)
                        .flatMap(total -> paymentAdminQueryRepository
                                .findByTenantId(tenantId, filter, pageQuery)
                                .collectList()
                                .map(content -> PagedResult.of(
                                        content,
                                        pageQuery.page(),
                                        pageQuery.size(),
                                        total
                                ))));
    }

    @Override
    public Mono<AdminPaymentView> execute(PaymentId paymentId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, paymentId).map(this::toView));
    }

    @Override
    public Mono<AdminPaymentView> execute(CreatePaymentCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> {
                    InvoiceId invoiceId = requireInvoiceId(command.invoiceId());
                    Money amount = Money.of(command.currency(), command.amountMinor());
                    PaymentMethodCode paymentMethodCode = toOptionalPaymentMethodCode(command.paymentMethodCode());

                    return validateInvoiceIssued(tenantId, invoiceId)
                            .then(Mono.defer(() -> {
                                Payment payment = Payment.create(
                                        PaymentId.generate(),
                                        tenantId,
                                        invoiceId,
                                        amount,
                                        paymentMethodCode,
                                        Instant.now()
                                );
                                return paymentRepository.save(payment).map(this::toView);
                            }));
                })
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminPaymentView> voidPayment(PaymentId paymentId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, paymentId)
                        .flatMap(payment -> {
                            payment.voidPayment();
                            return paymentRepository.save(payment);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    private Mono<Payment> loadInTenant(TenantId tenantId, PaymentId paymentId) {
        return paymentQueryPort.findByIdAndTenantId(paymentId, tenantId)
                .switchIfEmpty(Mono.error(new PaymentNotFoundException(
                        "Payment not found in tenant context")));
    }

    /**
     * Write-time Invoice ReferencePort validation (ADR-018 §create only). Maps this BC's
     * {@link InvoiceId}/{@link TenantId} UUIDs to the Billing contract's own value objects.
     */
    private Mono<Void> validateInvoiceIssued(TenantId tenantId, InvoiceId invoiceId) {
        com.codecore.billing.domain.valueobject.TenantId billingTenantId =
                new com.codecore.billing.domain.valueobject.TenantId(tenantId.value());
        com.codecore.billing.domain.valueobject.InvoiceId billingInvoiceId =
                new com.codecore.billing.domain.valueobject.InvoiceId(invoiceId.value());

        return invoiceReferencePort.existsIssuedByIdAndTenant(billingInvoiceId, billingTenantId)
                .flatMap(exists -> exists
                        ? Mono.<Void>empty()
                        : Mono.error(new InvoiceNotFoundException(
                                "Invoice not found, not in tenant, or not ISSUED")));
    }

    private static InvoiceId requireInvoiceId(java.util.UUID invoiceId) {
        if (invoiceId == null) {
            throw new InvalidDomainValueException("invoiceId is required");
        }
        return InvoiceId.of(invoiceId);
    }

    private static PaymentMethodCode toOptionalPaymentMethodCode(String paymentMethodCode) {
        return paymentMethodCode == null || paymentMethodCode.isBlank()
                ? null
                : PaymentMethodCode.of(paymentMethodCode);
    }

    private AdminPaymentView toView(Payment payment) {
        return new AdminPaymentView(
                payment.id(),
                payment.tenantId(),
                payment.invoiceId(),
                payment.amount().currency(),
                payment.amount().amountMinor(),
                payment.paymentMethodCode().map(PaymentMethodCode::value).orElse(null),
                payment.recordedAt(),
                payment.status(),
                payment.createdAt(),
                payment.updatedAt()
        );
    }
}
