package com.codecore.iam.application.authorization;

import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleName;

import java.util.Set;

import static com.codecore.iam.application.authorization.IamPermissionCatalog.ADMIN_IAM;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.MANAGER_IAM;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.MANAGER_ORGANIZATION;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.ORGANIZATION_PLATFORM_ALL;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.STRUCTURE_READ;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.union;

/**
 * Tenant-scoped system role templates provisioned on tenant creation (FASE 14.8 + 16.3 org contract).
 */
public enum SystemRoleTemplate {

    OWNER(
            RoleCode.of("OWNER"),
            RoleName.of("Owner"),
            IamPermissionCatalog.ALL
    ),
    ADMIN(
            RoleCode.of("ADMIN"),
            RoleName.of("Administrator"),
            union(ADMIN_IAM, ORGANIZATION_PLATFORM_ALL)
    ),
    MANAGER(
            RoleCode.of("MANAGER"),
            RoleName.of("Manager"),
            union(MANAGER_IAM, MANAGER_ORGANIZATION)
    ),
    USER(
            RoleCode.of("USER"),
            RoleName.of("User"),
            union(Set.of(IamPermissionCatalog.USER_READ), STRUCTURE_READ)
    ),
    READ_ONLY(
            RoleCode.of("READ_ONLY"),
            RoleName.of("Read only"),
            union(
                    Set.of(
                            IamPermissionCatalog.TENANT_READ,
                            IamPermissionCatalog.MEMBERSHIP_READ,
                            IamPermissionCatalog.ROLE_READ
                    ),
                    STRUCTURE_READ
            )
    );

    private final RoleCode code;
    private final RoleName roleName;
    private final Set<PermissionCode> permissions;

    SystemRoleTemplate(RoleCode code, RoleName roleName, Set<PermissionCode> permissions) {
        this.code = code;
        this.roleName = roleName;
        this.permissions = permissions;
    }

    public RoleCode code() {
        return code;
    }

    public RoleName roleName() {
        return roleName;
    }

    public Set<PermissionCode> permissions() {
        return permissions;
    }
}
