package com.codecore.iam.infrastructure.adapters.contract;

import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.contract.reference.IamSystemRoleReferencePort;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.TenantId;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class R2dbcIamSystemRoleReferenceAdapter implements IamSystemRoleReferencePort {

    private final RoleRepository roleRepository;

    public R2dbcIamSystemRoleReferenceAdapter(RoleRepository roleRepository) {
        this.roleRepository = Objects.requireNonNull(roleRepository, "roleRepository");
    }

    @Override
    public Mono<Boolean> existsSystemRoleByCodeAndTenant(String roleCode, TenantId tenantId) {
        return findSystemRole(roleCode, tenantId)
                .map(role -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<RoleId> findSystemRoleIdByCodeAndTenant(String roleCode, TenantId tenantId) {
        return findSystemRole(roleCode, tenantId)
                .map(Role::id);
    }

    private Mono<Role> findSystemRole(String roleCode, TenantId tenantId) {
        RoleCode code = RoleCode.of(roleCode);
        return roleRepository.findByTenantIdAndCode(tenantId, code)
                .filter(Role::systemRole);
    }
}
