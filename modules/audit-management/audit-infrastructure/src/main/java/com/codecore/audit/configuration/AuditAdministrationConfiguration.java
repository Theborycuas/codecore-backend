package com.codecore.audit.configuration;

import com.codecore.audit.application.admin.AuditAdministrationUseCaseImpl;
import com.codecore.audit.application.port.in.GetAuditEntryUseCase;
import com.codecore.audit.application.port.in.ListAuditEntriesUseCase;
import com.codecore.audit.application.port.out.AuditAdminQueryRepository;
import com.codecore.audit.application.port.out.AuditEntryQueryPort;
import com.codecore.audit.application.port.out.AuditEntryRepository;
import com.codecore.audit.application.port.out.TenantContextAccessor;
import com.codecore.audit.contract.append.AuditAppendPort;
import com.codecore.audit.infrastructure.adapters.IamTenantContextAccessor;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.contract.reference.IamMembershipReferencePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class AuditAdministrationConfiguration {

    @Bean
    public TenantContextAccessor auditTenantContextAccessor(
            AuthorizationContextAccessor authorizationContextAccessor
    ) {
        return new IamTenantContextAccessor(authorizationContextAccessor);
    }

    @Bean
    public AuditAdministrationUseCaseImpl auditAdministrationUseCase(
            TenantContextAccessor auditTenantContextAccessor,
            AuditAdminQueryRepository auditAdminQueryRepository,
            AuditEntryRepository auditEntryRepository,
            AuditEntryQueryPort auditEntryQueryPort,
            IamMembershipReferencePort iamMembershipReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        return new AuditAdministrationUseCaseImpl(
                auditTenantContextAccessor,
                auditAdminQueryRepository,
                auditEntryRepository,
                auditEntryQueryPort,
                iamMembershipReferencePort,
                transactionalOperator
        );
    }

    @Bean
    public ListAuditEntriesUseCase listAuditEntriesUseCase(AuditAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetAuditEntryUseCase getAuditEntryUseCase(AuditAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public AuditAppendPort auditAppendPort(AuditAdministrationUseCaseImpl delegate) {
        return delegate;
    }
}
