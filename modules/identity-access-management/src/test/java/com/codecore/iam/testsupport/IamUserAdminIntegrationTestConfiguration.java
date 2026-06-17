package com.codecore.iam.testsupport;

import com.codecore.iam.application.ReactorAuthorizationContextAccessor;
import com.codecore.iam.configuration.IamAdministrationConfiguration;
import com.codecore.iam.configuration.IamAuthenticationConfiguration;
import com.codecore.iam.configuration.IamAuthorizationConfiguration;
import com.codecore.iam.configuration.IamModuleConfiguration;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcAuthorizationQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcPermissionAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRoleAdminQueryRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcIdentityRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcMembershipRoleRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcPermissionRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRolePermissionRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcRoleRepository;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcTenantRepository;
import com.codecore.iam.infrastructure.security.BCryptPasswordHasher;
import com.codecore.iam.infrastructure.security.JwtTokenProvider;
import com.codecore.iam.infrastructure.security.JwtTokenValidator;
import com.codecore.iam.interfaces.http.AuthenticationController;
import com.codecore.iam.interfaces.http.IamHttpExceptionHandler;
import com.codecore.iam.interfaces.http.admin.IamMembershipAdminController;
import com.codecore.iam.interfaces.http.admin.IamPermissionAdminController;
import com.codecore.iam.interfaces.http.admin.IamRoleAdminController;
import com.codecore.iam.interfaces.http.admin.IamUserAdminController;
import com.codecore.iam.interfaces.http.security.AuthenticatedPrincipalAuthorizationManager;
import com.codecore.iam.interfaces.http.security.JwtAuthenticationWebFilter;
import com.codecore.iam.interfaces.http.security.RequiresPermissionAspect;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@Import({
        IamModuleConfiguration.class,
        IamAdministrationConfiguration.class,
        IamAuthenticationConfiguration.class,
        IamAuthorizationConfiguration.class,
        IamR2dbcTestConfiguration.class,
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
        R2dbcMembershipRoleRepository.class,
        R2dbcAuthorizationQueryRepository.class,
        BCryptPasswordHasher.class,
        JwtTokenProvider.class,
        JwtTokenValidator.class,
        JwtAuthenticationWebFilter.class,
        RequiresPermissionAspect.class,
        ReactorAuthorizationContextAccessor.class,
        AuthenticatedPrincipalAuthorizationManager.class,
        AuthenticationController.class,
        IamUserAdminController.class,
        IamMembershipAdminController.class,
        IamRoleAdminController.class,
        IamPermissionAdminController.class,
        IamHttpExceptionHandler.class
})
public class IamUserAdminIntegrationTestConfiguration {
}
