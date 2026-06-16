package com.codecore.iam.configuration;

import com.codecore.iam.application.AuthorizationServiceImpl;
import com.codecore.iam.application.authorization.TenantSystemRolesProvisionerImpl;
import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.application.port.out.RolePermissionRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.application.port.out.TenantSystemRolesProvisioner;
import com.codecore.iam.application.port.in.AuthorizationService;
import com.codecore.iam.application.port.out.AuthorizationQueryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Authorization evaluation wiring (FASE 14.5).
 */
@Configuration
@EnableAspectJAutoProxy
public class IamAuthorizationConfiguration {

    @Bean
    public AuthorizationService authorizationService(AuthorizationQueryRepository authorizationQueryRepository) {
        return new AuthorizationServiceImpl(authorizationQueryRepository);
    }

    @Bean
    public TenantSystemRolesProvisioner tenantSystemRolesProvisioner(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        return new TenantSystemRolesProvisionerImpl(
                roleRepository,
                permissionRepository,
                rolePermissionRepository
        );
    }
}
