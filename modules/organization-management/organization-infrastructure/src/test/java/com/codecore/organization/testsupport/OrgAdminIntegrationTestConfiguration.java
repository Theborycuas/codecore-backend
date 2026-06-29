package com.codecore.organization.testsupport;

import com.codecore.iam.configuration.IamAdministrationConfiguration;
import com.codecore.iam.configuration.IamAuthenticationConfiguration;
import com.codecore.iam.configuration.IamAuthorizationConfiguration;
import com.codecore.iam.configuration.IamBootstrapConfiguration;
import com.codecore.iam.configuration.IamModuleConfiguration;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcAuthorizationQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRoleAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRoleRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcPermissionAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcPermissionRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRoleAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRolePermissionAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRolePermissionRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRoleRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcTenantRepository;
import com.codecore.iam.interfaces.http.CreateTenantController;
import com.codecore.iam.infrastructure.security.BCryptPasswordHasher;
import com.codecore.iam.infrastructure.security.JwtTokenProvider;
import com.codecore.iam.infrastructure.security.JwtTokenValidator;
import com.codecore.iam.interfaces.http.AuthenticationController;
import com.codecore.iam.interfaces.http.IamHttpExceptionHandler;
import com.codecore.iam.interfaces.http.security.AuthorizationContextWebFilter;
import com.codecore.iam.interfaces.http.security.AuthenticatedPrincipalAuthorizationManager;
import com.codecore.iam.interfaces.http.security.JwtAuthenticationWebFilter;
import com.codecore.iam.interfaces.http.security.RequiresPermissionAspect;
import com.codecore.iam.application.ReactorAuthorizationContextAccessor;
import com.codecore.organization.testsupport.OrgR2dbcTestConfiguration;
import com.codecore.organization.configuration.OrganizationModuleConfiguration;
import com.codecore.organization.infrastructure.adapters.R2dbcMembershipReferenceAdapter;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcStaffAssignmentAdminQueryRepository;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcStaffAssignmentRepository;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcOfficeAdminQueryRepository;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcOfficeRepository;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcOrganizationAdminQueryRepository;
import com.codecore.organization.infrastructure.persistence.repository.R2dbcOrganizationRepository;
import com.codecore.organization.interfaces.http.OrgHttpExceptionHandler;
import com.codecore.organization.interfaces.http.admin.OfficeAdminController;
import com.codecore.organization.interfaces.http.admin.StaffAssignmentAdminController;
import com.codecore.organization.interfaces.http.admin.OrganizationAdminController;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@Import({
        IamModuleConfiguration.class,
        IamBootstrapConfiguration.class,
        IamAdministrationConfiguration.class,
        IamAuthenticationConfiguration.class,
        IamAuthorizationConfiguration.class,
        OrgR2dbcTestConfiguration.class,
        OrganizationModuleConfiguration.class,
        R2dbcOrganizationRepository.class,
        R2dbcOrganizationAdminQueryRepository.class,
        R2dbcOfficeRepository.class,
        R2dbcOfficeAdminQueryRepository.class,
        R2dbcStaffAssignmentRepository.class,
        R2dbcStaffAssignmentAdminQueryRepository.class,
        R2dbcMembershipReferenceAdapter.class,
        R2dbcIdentityRepository.class,
        R2dbcIdentityAdminQueryRepository.class,
        R2dbcMembershipAdminQueryRepository.class,
        R2dbcRoleAdminQueryRepository.class,
        R2dbcPermissionAdminQueryRepository.class,
        R2dbcTenantRepository.class,
        R2dbcMembershipRepository.class,
        R2dbcRoleRepository.class,
        R2dbcPermissionRepository.class,
        R2dbcRolePermissionRepository.class,
        R2dbcRolePermissionAdminQueryRepository.class,
        R2dbcMembershipRoleRepository.class,
        R2dbcMembershipRoleAdminQueryRepository.class,
        R2dbcAuthorizationQueryRepository.class,
        BCryptPasswordHasher.class,
        JwtTokenProvider.class,
        JwtTokenValidator.class,
        JwtAuthenticationWebFilter.class,
        AuthorizationContextWebFilter.class,
        RequiresPermissionAspect.class,
        ReactorAuthorizationContextAccessor.class,
        AuthenticatedPrincipalAuthorizationManager.class,
        AuthenticationController.class,
        CreateTenantController.class,
        OrganizationAdminController.class,
        OfficeAdminController.class,
        StaffAssignmentAdminController.class,
        IamHttpExceptionHandler.class,
        OrgHttpExceptionHandler.class
})
public class OrgAdminIntegrationTestConfiguration {
}
