package com.codecore.iam.configuration;

import com.codecore.iam.application.admin.IdentityRegistrationOrchestrator;
import com.codecore.iam.application.admin.OwnershipPolicy;
import com.codecore.iam.application.admin.MembershipAdministrationUseCaseImpl;
import com.codecore.iam.application.admin.PermissionAdministrationUseCaseImpl;
import com.codecore.iam.application.admin.RoleAdministrationUseCaseImpl;
import com.codecore.iam.application.admin.UserAdministrationUseCaseImpl;
import com.codecore.iam.application.port.in.CreateAdminMembershipUseCase;
import com.codecore.iam.application.port.in.CreateAdminRoleUseCase;
import com.codecore.iam.application.port.in.CreateAdminUserUseCase;
import com.codecore.iam.application.port.in.DeactivateAdminMembershipUseCase;
import com.codecore.iam.application.port.in.DeactivateAdminUserUseCase;
import com.codecore.iam.application.port.in.DeleteAdminRoleUseCase;
import com.codecore.iam.application.port.in.GetAdminMembershipUseCase;
import com.codecore.iam.application.port.in.GetAdminPermissionUseCase;
import com.codecore.iam.application.port.in.GetAdminRoleUseCase;
import com.codecore.iam.application.port.in.GetAdminUserUseCase;
import com.codecore.iam.application.port.in.ListAdminMembershipsUseCase;
import com.codecore.iam.application.port.in.ListAdminPermissionsUseCase;
import com.codecore.iam.application.port.in.ListAdminRolesUseCase;
import com.codecore.iam.application.port.in.ListAdminUsersUseCase;
import com.codecore.iam.application.port.in.UpdateAdminMembershipUseCase;
import com.codecore.iam.application.port.in.UpdateAdminRoleUseCase;
import com.codecore.iam.application.port.in.UpdateAdminUserUseCase;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.IdentityAdminQueryRepository;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipAdminQueryRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.PermissionAdminQueryRepository;
import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.application.port.out.RoleAdminQueryRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class IamAdministrationConfiguration {

    @Bean
    public OwnershipPolicy ownershipPolicy(
            MembershipRepository membershipRepository,
            MembershipRoleRepository membershipRoleRepository,
            RoleRepository roleRepository
    ) {
        return new OwnershipPolicy(membershipRepository, membershipRoleRepository, roleRepository);
    }

    @Bean
    public UserAdministrationUseCaseImpl userAdministrationUseCase(
            AuthorizationContextAccessor authorizationContextAccessor,
            IdentityAdminQueryRepository identityAdminQueryRepository,
            IdentityRepository identityRepository,
            MembershipRepository membershipRepository,
            IdentityRegistrationOrchestrator identityRegistrationOrchestrator,
            OwnershipPolicy ownershipPolicy,
            TransactionalOperator transactionalOperator
    ) {
        return new UserAdministrationUseCaseImpl(
                authorizationContextAccessor,
                identityAdminQueryRepository,
                identityRepository,
                membershipRepository,
                identityRegistrationOrchestrator,
                ownershipPolicy,
                transactionalOperator
        );
    }

    @Bean
    public ListAdminUsersUseCase listAdminUsersUseCase(UserAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetAdminUserUseCase getAdminUserUseCase(UserAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CreateAdminUserUseCase createAdminUserUseCase(UserAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public UpdateAdminUserUseCase updateAdminUserUseCase(UserAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public DeactivateAdminUserUseCase deactivateAdminUserUseCase(UserAdministrationUseCaseImpl delegate) {
        return delegate::deactivate;
    }

    @Bean
    public MembershipAdministrationUseCaseImpl membershipAdministrationUseCase(
            AuthorizationContextAccessor authorizationContextAccessor,
            MembershipAdminQueryRepository membershipAdminQueryRepository,
            MembershipRepository membershipRepository,
            IdentityRepository identityRepository,
            IdentityRegistrationOrchestrator identityRegistrationOrchestrator,
            OwnershipPolicy ownershipPolicy,
            TransactionalOperator transactionalOperator
    ) {
        return new MembershipAdministrationUseCaseImpl(
                authorizationContextAccessor,
                membershipAdminQueryRepository,
                membershipRepository,
                identityRepository,
                identityRegistrationOrchestrator,
                ownershipPolicy,
                transactionalOperator
        );
    }

    @Bean
    public ListAdminMembershipsUseCase listAdminMembershipsUseCase(MembershipAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetAdminMembershipUseCase getAdminMembershipUseCase(MembershipAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CreateAdminMembershipUseCase createAdminMembershipUseCase(MembershipAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public UpdateAdminMembershipUseCase updateAdminMembershipUseCase(MembershipAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public DeactivateAdminMembershipUseCase deactivateAdminMembershipUseCase(
            MembershipAdministrationUseCaseImpl delegate
    ) {
        return delegate::deactivate;
    }

    @Bean
    public RoleAdministrationUseCaseImpl roleAdministrationUseCase(
            AuthorizationContextAccessor authorizationContextAccessor,
            RoleAdminQueryRepository roleAdminQueryRepository,
            RoleRepository roleRepository,
            MembershipRoleRepository membershipRoleRepository,
            TransactionalOperator transactionalOperator
    ) {
        return new RoleAdministrationUseCaseImpl(
                authorizationContextAccessor,
                roleAdminQueryRepository,
                roleRepository,
                membershipRoleRepository,
                transactionalOperator
        );
    }

    @Bean
    public ListAdminRolesUseCase listAdminRolesUseCase(RoleAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetAdminRoleUseCase getAdminRoleUseCase(RoleAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public CreateAdminRoleUseCase createAdminRoleUseCase(RoleAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public UpdateAdminRoleUseCase updateAdminRoleUseCase(RoleAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public DeleteAdminRoleUseCase deleteAdminRoleUseCase(RoleAdministrationUseCaseImpl delegate) {
        return delegate::delete;
    }

    @Bean
    public PermissionAdministrationUseCaseImpl permissionAdministrationUseCase(
            AuthorizationContextAccessor authorizationContextAccessor,
            PermissionAdminQueryRepository permissionAdminQueryRepository,
            PermissionRepository permissionRepository
    ) {
        return new PermissionAdministrationUseCaseImpl(
                authorizationContextAccessor,
                permissionAdminQueryRepository,
                permissionRepository
        );
    }

    @Bean
    public ListAdminPermissionsUseCase listAdminPermissionsUseCase(PermissionAdministrationUseCaseImpl delegate) {
        return delegate;
    }

    @Bean
    public GetAdminPermissionUseCase getAdminPermissionUseCase(PermissionAdministrationUseCaseImpl delegate) {
        return delegate;
    }
}
