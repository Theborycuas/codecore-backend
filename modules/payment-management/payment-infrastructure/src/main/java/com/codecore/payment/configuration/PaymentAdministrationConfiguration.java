package com.codecore.payment.configuration;

import com.codecore.billing.contract.reference.InvoiceReferencePort;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.payment.application.admin.PaymentAdministrationUseCaseImpl;
import com.codecore.payment.application.port.in.CreatePaymentUseCase;
import com.codecore.payment.application.port.in.GetPaymentUseCase;
import com.codecore.payment.application.port.in.ListPaymentsUseCase;
import com.codecore.payment.application.port.in.VoidPaymentUseCase;
import com.codecore.payment.application.port.out.PaymentAdminQueryRepository;
import com.codecore.payment.application.port.out.PaymentQueryPort;
import com.codecore.payment.application.port.out.PaymentRepository;
import com.codecore.payment.application.port.out.TenantContextAccessor;
import com.codecore.payment.infrastructure.adapters.IamTenantContextAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class PaymentAdministrationConfiguration {

    @Bean
    public TenantContextAccessor paymentTenantContextAccessor(
            AuthorizationContextAccessor authorizationContextAccessor
    ) {
        return new IamTenantContextAccessor(authorizationContextAccessor);
    }

    @Bean
    public PaymentAdministrationUseCaseImpl paymentAdministrationUseCase(
            TenantContextAccessor paymentTenantContextAccessor,
            PaymentAdminQueryRepository paymentAdminQueryRepository,
            PaymentRepository paymentRepository,
            PaymentQueryPort paymentQueryPort,
            InvoiceReferencePort invoiceReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        return new PaymentAdministrationUseCaseImpl(
                paymentTenantContextAccessor,
                paymentAdminQueryRepository,
                paymentRepository,
                paymentQueryPort,
                invoiceReferencePort,
                transactionalOperator
        );
    }

    @Bean
    public ListPaymentsUseCase listPaymentsUseCase(PaymentAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetPaymentUseCase getPaymentUseCase(PaymentAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CreatePaymentUseCase createPaymentUseCase(PaymentAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public VoidPaymentUseCase voidPaymentUseCase(PaymentAdministrationUseCaseImpl delegate) {
        return delegate;
    }
}
