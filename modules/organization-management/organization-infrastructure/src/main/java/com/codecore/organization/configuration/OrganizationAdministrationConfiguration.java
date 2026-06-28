package com.codecore.organization.configuration;

import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.organization.application.admin.OfficeAdministrationUseCaseImpl;
import com.codecore.organization.application.admin.OrganizationAdministrationUseCaseImpl;
import com.codecore.organization.application.port.in.ActivateOfficeUseCase;
import com.codecore.organization.application.port.in.ActivateOrganizationUseCase;
import com.codecore.organization.application.port.in.ArchiveOfficeUseCase;
import com.codecore.organization.application.port.in.ArchiveOrganizationUseCase;
import com.codecore.organization.application.port.in.CreateOfficeUseCase;
import com.codecore.organization.application.port.in.CreateOrganizationUseCase;
import com.codecore.organization.application.port.in.GetOfficeUseCase;
import com.codecore.organization.application.port.in.GetOrganizationUseCase;
import com.codecore.organization.application.port.in.ListOfficesUseCase;
import com.codecore.organization.application.port.in.ListOrganizationsUseCase;
import com.codecore.organization.application.port.in.UpdateOfficeUseCase;
import com.codecore.organization.application.port.in.UpdateOrganizationUseCase;
import com.codecore.organization.application.port.out.OfficeAdminQueryRepository;
import com.codecore.organization.application.port.out.OfficeQueryPort;
import com.codecore.organization.application.port.out.OfficeRepository;
import com.codecore.organization.application.port.out.OrganizationAdminQueryRepository;
import com.codecore.organization.application.port.out.OrganizationQueryPort;
import com.codecore.organization.application.port.out.OrganizationRepository;
import com.codecore.organization.application.port.out.TenantContextAccessor;
import com.codecore.organization.infrastructure.adapters.IamTenantContextAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class OrganizationAdministrationConfiguration {

    @Bean
    public TenantContextAccessor tenantContextAccessor(AuthorizationContextAccessor authorizationContextAccessor) {
        return new IamTenantContextAccessor(authorizationContextAccessor);
    }

    @Bean
    public OrganizationAdministrationUseCaseImpl organizationAdministrationUseCase(
            TenantContextAccessor tenantContextAccessor,
            OrganizationAdminQueryRepository organizationAdminQueryRepository,
            OrganizationRepository organizationRepository,
            OrganizationQueryPort organizationQueryPort,
            OfficeRepository officeRepository,
            TransactionalOperator transactionalOperator
    ) {
        return new OrganizationAdministrationUseCaseImpl(
                tenantContextAccessor,
                organizationAdminQueryRepository,
                organizationRepository,
                organizationQueryPort,
                officeRepository,
                transactionalOperator
        );
    }

    @Bean
    public ListOrganizationsUseCase listOrganizationsUseCase(OrganizationAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetOrganizationUseCase getOrganizationUseCase(OrganizationAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CreateOrganizationUseCase createOrganizationUseCase(OrganizationAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public UpdateOrganizationUseCase updateOrganizationUseCase(OrganizationAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public ArchiveOrganizationUseCase archiveOrganizationUseCase(OrganizationAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public ActivateOrganizationUseCase activateOrganizationUseCase(OrganizationAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public OfficeAdministrationUseCaseImpl officeAdministrationUseCase(
            TenantContextAccessor tenantContextAccessor,
            OfficeAdminQueryRepository officeAdminQueryRepository,
            OfficeRepository officeRepository,
            OfficeQueryPort officeQueryPort,
            OrganizationQueryPort organizationQueryPort,
            TransactionalOperator transactionalOperator
    ) {
        return new OfficeAdministrationUseCaseImpl(
                tenantContextAccessor,
                officeAdminQueryRepository,
                officeRepository,
                officeQueryPort,
                organizationQueryPort,
                transactionalOperator
        );
    }

    @Bean
    public ListOfficesUseCase listOfficesUseCase(OfficeAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetOfficeUseCase getOfficeUseCase(OfficeAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CreateOfficeUseCase createOfficeUseCase(OfficeAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public UpdateOfficeUseCase updateOfficeUseCase(OfficeAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public ArchiveOfficeUseCase archiveOfficeUseCase(OfficeAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public ActivateOfficeUseCase activateOfficeUseCase(OfficeAdministrationUseCaseImpl delegate) {
        return delegate;
    }
}
