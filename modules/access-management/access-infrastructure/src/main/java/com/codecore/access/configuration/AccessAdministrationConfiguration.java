package com.codecore.access.configuration;

import com.codecore.access.application.admin.InvitationAdministrationUseCaseImpl;
import com.codecore.access.application.port.in.AcceptInvitationUseCase;
import com.codecore.access.application.port.in.CreateInvitationUseCase;
import com.codecore.access.application.port.in.GetInvitationUseCase;
import com.codecore.access.application.port.in.ListInvitationsUseCase;
import com.codecore.access.application.port.in.RevokeInvitationUseCase;
import com.codecore.access.application.port.out.InvitationAdminQueryRepository;
import com.codecore.access.application.port.out.InvitationQueryPort;
import com.codecore.access.application.port.out.InvitationRepository;
import com.codecore.access.application.port.out.MembershipContextAccessor;
import com.codecore.access.application.port.out.SendInvitationEmailPort;
import com.codecore.access.application.port.out.TenantContextAccessor;
import com.codecore.access.infrastructure.adapters.IamMembershipContextAccessor;
import com.codecore.access.infrastructure.adapters.IamTenantContextAccessor;
import com.codecore.audit.contract.append.AuditAppendPort;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.contract.provision.TenantAccessProvisionPort;
import com.codecore.iam.contract.reference.IamActiveMembershipByEmailPort;
import com.codecore.iam.contract.reference.IamMembershipReferencePort;
import com.codecore.iam.contract.reference.IamSystemRoleReferencePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class AccessAdministrationConfiguration {

    @Bean
    public TenantContextAccessor accessTenantContextAccessor(
            AuthorizationContextAccessor authorizationContextAccessor
    ) {
        return new IamTenantContextAccessor(authorizationContextAccessor);
    }

    @Bean
    public MembershipContextAccessor accessMembershipContextAccessor(
            AuthorizationContextAccessor authorizationContextAccessor
    ) {
        return new IamMembershipContextAccessor(authorizationContextAccessor);
    }

    @Bean
    public InvitationAdministrationUseCaseImpl invitationAdministrationUseCase(
            TenantContextAccessor accessTenantContextAccessor,
            MembershipContextAccessor accessMembershipContextAccessor,
            InvitationAdminQueryRepository invitationAdminQueryRepository,
            InvitationRepository invitationRepository,
            InvitationQueryPort invitationQueryPort,
            IamMembershipReferencePort iamMembershipReferencePort,
            IamActiveMembershipByEmailPort iamActiveMembershipByEmailPort,
            IamSystemRoleReferencePort iamSystemRoleReferencePort,
            TenantAccessProvisionPort tenantAccessProvisionPort,
            SendInvitationEmailPort sendInvitationEmailPort,
            AuditAppendPort auditAppendPort,
            TransactionalOperator transactionalOperator
    ) {
        return new InvitationAdministrationUseCaseImpl(
                accessTenantContextAccessor,
                accessMembershipContextAccessor,
                invitationAdminQueryRepository,
                invitationRepository,
                invitationQueryPort,
                iamMembershipReferencePort,
                iamActiveMembershipByEmailPort,
                iamSystemRoleReferencePort,
                tenantAccessProvisionPort,
                sendInvitationEmailPort,
                auditAppendPort,
                transactionalOperator
        );
    }

    @Bean
    public ListInvitationsUseCase listInvitationsUseCase(InvitationAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetInvitationUseCase getInvitationUseCase(InvitationAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CreateInvitationUseCase createInvitationUseCase(InvitationAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public RevokeInvitationUseCase revokeInvitationUseCase(InvitationAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public AcceptInvitationUseCase acceptInvitationUseCase(InvitationAdministrationUseCaseImpl delegate) {
        return delegate;
    }
}
