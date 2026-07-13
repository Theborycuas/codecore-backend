package com.codecore.billing.configuration;

import com.codecore.billing.application.admin.InvoiceAdministrationUseCaseImpl;
import com.codecore.billing.application.port.in.CreateInvoiceUseCase;
import com.codecore.billing.application.port.in.GetInvoiceUseCase;
import com.codecore.billing.application.port.in.IssueInvoiceUseCase;
import com.codecore.billing.application.port.in.ListInvoicesUseCase;
import com.codecore.billing.application.port.in.UpdateInvoiceUseCase;
import com.codecore.billing.application.port.in.VoidInvoiceUseCase;
import com.codecore.billing.application.port.out.InvoiceAdminQueryRepository;
import com.codecore.billing.application.port.out.InvoiceQueryPort;
import com.codecore.billing.application.port.out.InvoiceRepository;
import com.codecore.billing.application.port.out.TenantContextAccessor;
import com.codecore.billing.infrastructure.adapters.IamTenantContextAccessor;
import com.codecore.encounter.contract.reference.EncounterReferencePort;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.inventory.contract.reference.ItemReferencePort;
import com.codecore.organization.contract.reference.OrganizationReferencePort;
import com.codecore.patient.contract.reference.PatientReferencePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class BillingAdministrationConfiguration {

    @Bean
    public TenantContextAccessor invoiceTenantContextAccessor(
            AuthorizationContextAccessor authorizationContextAccessor
    ) {
        return new IamTenantContextAccessor(authorizationContextAccessor);
    }

    @Bean
    public InvoiceAdministrationUseCaseImpl invoiceAdministrationUseCase(
            TenantContextAccessor invoiceTenantContextAccessor,
            InvoiceAdminQueryRepository invoiceAdminQueryRepository,
            InvoiceRepository invoiceRepository,
            InvoiceQueryPort invoiceQueryPort,
            OrganizationReferencePort organizationReferencePort,
            PatientReferencePort patientReferencePort,
            ItemReferencePort itemReferencePort,
            EncounterReferencePort encounterReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        return new InvoiceAdministrationUseCaseImpl(
                invoiceTenantContextAccessor,
                invoiceAdminQueryRepository,
                invoiceRepository,
                invoiceQueryPort,
                organizationReferencePort,
                patientReferencePort,
                itemReferencePort,
                encounterReferencePort,
                transactionalOperator
        );
    }

    @Bean
    public ListInvoicesUseCase listInvoicesUseCase(InvoiceAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetInvoiceUseCase getInvoiceUseCase(InvoiceAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CreateInvoiceUseCase createInvoiceUseCase(InvoiceAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public UpdateInvoiceUseCase updateInvoiceUseCase(InvoiceAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public IssueInvoiceUseCase issueInvoiceUseCase(InvoiceAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public VoidInvoiceUseCase voidInvoiceUseCase(InvoiceAdministrationUseCaseImpl delegate) {
        return delegate;
    }
}
