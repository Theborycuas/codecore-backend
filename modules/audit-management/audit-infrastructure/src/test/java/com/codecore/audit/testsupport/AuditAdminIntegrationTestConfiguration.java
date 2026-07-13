package com.codecore.audit.testsupport;

import com.codecore.access.configuration.AccessModuleConfiguration;
import com.codecore.access.infrastructure.adapters.LoggingSendInvitationEmailAdapter;
import com.codecore.access.infrastructure.adapters.R2dbcInvitationReferenceAdapter;
import com.codecore.access.infrastructure.persistence.repository.R2dbcInvitationAdminQueryRepository;
import com.codecore.access.infrastructure.persistence.repository.R2dbcInvitationRepository;
import com.codecore.access.interfaces.http.AccessHttpExceptionHandler;
import com.codecore.access.interfaces.http.admin.InvitationAdminController;
import com.codecore.access.interfaces.http.publicapi.InvitationAcceptController;
import com.codecore.audit.configuration.AuditModuleConfiguration;
import com.codecore.audit.infrastructure.adapters.R2dbcAuditReferenceAdapter;
import com.codecore.audit.infrastructure.persistence.repository.R2dbcAuditAdminQueryRepository;
import com.codecore.audit.infrastructure.persistence.repository.R2dbcAuditEntryRepository;
import com.codecore.audit.interfaces.http.AuditHttpExceptionHandler;
import com.codecore.audit.interfaces.http.admin.AuditAdminController;
import com.codecore.iam.application.ReactorAuthorizationContextAccessor;
import com.codecore.iam.configuration.IamAdministrationConfiguration;
import com.codecore.iam.configuration.IamAuthenticationConfiguration;
import com.codecore.iam.configuration.IamAuthorizationConfiguration;
import com.codecore.iam.configuration.IamBootstrapConfiguration;
import com.codecore.iam.configuration.IamModuleConfiguration;
import com.codecore.iam.infrastructure.adapters.contract.R2dbcIamActiveMembershipByEmailAdapter;
import com.codecore.iam.infrastructure.adapters.contract.R2dbcIamMembershipReferenceAdapter;
import com.codecore.iam.infrastructure.adapters.contract.R2dbcIamSystemRoleReferenceAdapter;
import com.codecore.iam.infrastructure.adapters.contract.TenantAccessProvisionAdapter;
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
import com.codecore.iam.infrastructure.security.BCryptPasswordHasher;
import com.codecore.iam.infrastructure.security.JwtTokenProvider;
import com.codecore.iam.infrastructure.security.JwtTokenValidator;
import com.codecore.iam.interfaces.http.AuthenticationController;
import com.codecore.iam.interfaces.http.CreateTenantController;
import com.codecore.iam.interfaces.http.IamHttpExceptionHandler;
import com.codecore.iam.interfaces.http.security.AuthenticatedPrincipalAuthorizationManager;
import com.codecore.iam.interfaces.http.security.AuthorizationContextWebFilter;
import com.codecore.iam.interfaces.http.security.JwtAuthenticationWebFilter;
import com.codecore.iam.interfaces.http.security.RequiresPermissionAspect;
import com.codecore.platform.r2dbc.PlatformR2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Full Audit + Access + IAM stack for FASE 24.7 verification (E2E HTTP + producers).
 */
@Configuration
@EnableAutoConfiguration
@Import({
        IamModuleConfiguration.class,
        IamBootstrapConfiguration.class,
        IamAdministrationConfiguration.class,
        IamAuthenticationConfiguration.class,
        IamAuthorizationConfiguration.class,
        PlatformR2dbcAutoConfiguration.class,
        AccessModuleConfiguration.class,
        AuditModuleConfiguration.class,
        R2dbcInvitationRepository.class,
        R2dbcInvitationAdminQueryRepository.class,
        R2dbcInvitationReferenceAdapter.class,
        LoggingSendInvitationEmailAdapter.class,
        R2dbcAuditEntryRepository.class,
        R2dbcAuditAdminQueryRepository.class,
        R2dbcAuditReferenceAdapter.class,
        R2dbcIamMembershipReferenceAdapter.class,
        R2dbcIamActiveMembershipByEmailAdapter.class,
        R2dbcIamSystemRoleReferenceAdapter.class,
        TenantAccessProvisionAdapter.class,
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
        InvitationAdminController.class,
        InvitationAcceptController.class,
        AuditAdminController.class,
        IamHttpExceptionHandler.class,
        AccessHttpExceptionHandler.class,
        AuditHttpExceptionHandler.class
})
public class AuditAdminIntegrationTestConfiguration {
}
