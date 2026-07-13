package com.codecore.iam.configuration;

import com.codecore.iam.application.CreateTenantUseCaseImpl;
import com.codecore.iam.application.RegisterIdentityUseCaseImpl;
import com.codecore.iam.application.CompletePasswordResetUseCaseImpl;
import com.codecore.iam.application.RequestPasswordResetUseCaseImpl;
import com.codecore.iam.application.admin.IdentityRegistrationOrchestrator;
import com.codecore.iam.application.port.in.CompletePasswordResetUseCase;
import com.codecore.iam.application.port.in.CreateTenantUseCase;
import com.codecore.iam.application.port.in.RegisterIdentityUseCase;
import com.codecore.iam.application.port.in.RequestPasswordResetUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.application.port.out.PasswordResetRepository;
import com.codecore.iam.application.port.out.SendPasswordResetEmailPort;
import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.application.port.out.TenantSystemRolesProvisioner;
import com.codecore.iam.infrastructure.adapters.LoggingSendPasswordResetEmailAdapter;
import com.codecore.iam.infrastructure.persistence.mapper.IamIdentityTenantMembershipMapper;
import com.codecore.iam.infrastructure.persistence.mapper.IamPasswordResetRequestMapper;
import com.codecore.iam.infrastructure.persistence.mapper.IamPermissionMapper;
import com.codecore.iam.infrastructure.persistence.mapper.IamRoleMapper;
import com.codecore.iam.infrastructure.persistence.mapper.IamTenantMapper;
import com.codecore.iam.infrastructure.persistence.mapper.IamUserMapper;
import com.codecore.iam.infrastructure.persistence.repository.R2dbcPasswordResetRepository;
import com.codecore.iam.infrastructure.persistence.repository.SpringDataIamPasswordResetRequestRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * IAM module Spring entry point. Registers persistence adapters and registration use case.
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.codecore.iam.infrastructure.persistence.repository")
public class IamModuleConfiguration {

    @Bean
    public IamUserMapper iamUserMapper() {
        return new IamUserMapper();
    }

    @Bean
    public IamTenantMapper iamTenantMapper() {
        return new IamTenantMapper();
    }

    @Bean
    public IamIdentityTenantMembershipMapper iamIdentityTenantMembershipMapper() {
        return new IamIdentityTenantMembershipMapper();
    }

    @Bean
    public IamRoleMapper iamRoleMapper() {
        return new IamRoleMapper();
    }

    @Bean
    public IamPermissionMapper iamPermissionMapper() {
        return new IamPermissionMapper();
    }

    @Bean
    public IamPasswordResetRequestMapper iamPasswordResetRequestMapper() {
        return new IamPasswordResetRequestMapper();
    }

    @Bean
    public PasswordResetRepository passwordResetRepository(
            SpringDataIamPasswordResetRequestRepository springDataRepository,
            IamPasswordResetRequestMapper iamPasswordResetRequestMapper
    ) {
        return new R2dbcPasswordResetRepository(springDataRepository, iamPasswordResetRequestMapper);
    }

    @Bean
    public SendPasswordResetEmailPort sendPasswordResetEmailPort() {
        return new LoggingSendPasswordResetEmailAdapter();
    }

    @Bean
    public IdentityRegistrationOrchestrator identityRegistrationOrchestrator(
            IdentityRepository identityRepository,
            MembershipRepository membershipRepository,
            PasswordHasher passwordHasher,
            TransactionalOperator transactionalOperator
    ) {
        return new IdentityRegistrationOrchestrator(
                identityRepository,
                membershipRepository,
                passwordHasher,
                transactionalOperator
        );
    }

    @Bean
    public RegisterIdentityUseCase registerIdentityUseCase(
            IdentityRepository identityRepository,
            IdentityRegistrationOrchestrator identityRegistrationOrchestrator
    ) {
        return new RegisterIdentityUseCaseImpl(
                identityRepository,
                identityRegistrationOrchestrator
        );
    }

    @Bean
    public CreateTenantUseCase createTenantUseCase(
            TenantRepository tenantRepository,
            TenantSystemRolesProvisioner tenantSystemRolesProvisioner
    ) {
        return new CreateTenantUseCaseImpl(tenantRepository, tenantSystemRolesProvisioner);
    }

    @Bean
    public RequestPasswordResetUseCase requestPasswordResetUseCase(
            IdentityRepository identityRepository,
            MembershipRepository membershipRepository,
            PasswordResetRepository passwordResetRepository,
            SendPasswordResetEmailPort sendPasswordResetEmailPort,
            TransactionalOperator transactionalOperator
    ) {
        return new RequestPasswordResetUseCaseImpl(
                identityRepository,
                membershipRepository,
                passwordResetRepository,
                sendPasswordResetEmailPort,
                transactionalOperator
        );
    }

    @Bean
    public CompletePasswordResetUseCase completePasswordResetUseCase(
            PasswordResetRepository passwordResetRepository,
            IdentityRepository identityRepository,
            PasswordHasher passwordHasher,
            TransactionalOperator transactionalOperator
    ) {
        return new CompletePasswordResetUseCaseImpl(
                passwordResetRepository,
                identityRepository,
                passwordHasher,
                transactionalOperator
        );
    }
}
