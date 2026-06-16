package com.codecore.iam.application.authorization;

import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleName;

import java.util.Set;

/**
 * Tenant-scoped system role templates provisioned on tenant creation (FASE 14.8).
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
            Set.of(
                    IamPermissionCatalog.MEMBERSHIP_READ,
                    IamPermissionCatalog.MEMBERSHIP_CREATE,
                    IamPermissionCatalog.MEMBERSHIP_UPDATE,
                    IamPermissionCatalog.MEMBERSHIP_DELETE,
                    IamPermissionCatalog.ROLE_READ,
                    IamPermissionCatalog.ROLE_CREATE,
                    IamPermissionCatalog.ROLE_UPDATE,
                    IamPermissionCatalog.ROLE_DELETE,
                    IamPermissionCatalog.PERMISSION_ASSIGN,
                    IamPermissionCatalog.USER_READ,
                    IamPermissionCatalog.USER_CREATE,
                    IamPermissionCatalog.USER_UPDATE,
                    IamPermissionCatalog.USER_DELETE
            )
    ),
    MANAGER(
            RoleCode.of("MANAGER"),
            RoleName.of("Manager"),
            Set.of(
                    IamPermissionCatalog.MEMBERSHIP_READ,
                    IamPermissionCatalog.USER_READ,
                    IamPermissionCatalog.USER_UPDATE
            )
    ),
    USER(
            RoleCode.of("USER"),
            RoleName.of("User"),
            Set.of(IamPermissionCatalog.USER_READ)
    ),
    READ_ONLY(
            RoleCode.of("READ_ONLY"),
            RoleName.of("Read only"),
            Set.of(
                    IamPermissionCatalog.TENANT_READ,
                    IamPermissionCatalog.MEMBERSHIP_READ,
                    IamPermissionCatalog.ROLE_READ
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
