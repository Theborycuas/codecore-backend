package com.codecore.iam.contract.reference;

import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

/**
 * Cross-BC reference: tenant-scoped system roles (code + {@code system_role=true}).
 */
public interface IamSystemRoleReferencePort {

    Mono<Boolean> existsSystemRoleByCodeAndTenant(String roleCode, TenantId tenantId);

    Mono<RoleId> findSystemRoleIdByCodeAndTenant(String roleCode, TenantId tenantId);
}
