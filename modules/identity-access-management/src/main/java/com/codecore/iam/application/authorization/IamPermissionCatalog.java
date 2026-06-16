package com.codecore.iam.application.authorization;

import com.codecore.iam.domain.valueobject.PermissionCode;

import java.util.Set;

/**
 * Platform IAM permission contract (FASE 14.8). No business-module grants.
 */
public final class IamPermissionCatalog {

    public static final PermissionCode TENANT_READ = PermissionCode.of("tenant:read");
    public static final PermissionCode TENANT_UPDATE = PermissionCode.of("tenant:update");

    public static final PermissionCode MEMBERSHIP_READ = PermissionCode.of("membership:read");
    public static final PermissionCode MEMBERSHIP_CREATE = PermissionCode.of("membership:create");
    public static final PermissionCode MEMBERSHIP_UPDATE = PermissionCode.of("membership:update");
    public static final PermissionCode MEMBERSHIP_DELETE = PermissionCode.of("membership:delete");

    public static final PermissionCode ROLE_READ = PermissionCode.of("role:read");
    public static final PermissionCode ROLE_CREATE = PermissionCode.of("role:create");
    public static final PermissionCode ROLE_UPDATE = PermissionCode.of("role:update");
    public static final PermissionCode ROLE_DELETE = PermissionCode.of("role:delete");

    public static final PermissionCode PERMISSION_READ = PermissionCode.of("permission:read");
    public static final PermissionCode PERMISSION_ASSIGN = PermissionCode.of("permission:assign");

    public static final PermissionCode USER_READ = PermissionCode.of("user:read");
    public static final PermissionCode USER_CREATE = PermissionCode.of("user:create");
    public static final PermissionCode USER_UPDATE = PermissionCode.of("user:update");
    public static final PermissionCode USER_DELETE = PermissionCode.of("user:delete");

    public static final Set<PermissionCode> ALL = Set.of(
            TENANT_READ,
            TENANT_UPDATE,
            MEMBERSHIP_READ,
            MEMBERSHIP_CREATE,
            MEMBERSHIP_UPDATE,
            MEMBERSHIP_DELETE,
            ROLE_READ,
            ROLE_CREATE,
            ROLE_UPDATE,
            ROLE_DELETE,
            PERMISSION_READ,
            PERMISSION_ASSIGN,
            USER_READ,
            USER_CREATE,
            USER_UPDATE,
            USER_DELETE
    );

    public static final Set<PermissionCode> MEMBERSHIP_ALL = Set.of(
            MEMBERSHIP_READ,
            MEMBERSHIP_CREATE,
            MEMBERSHIP_UPDATE,
            MEMBERSHIP_DELETE
    );

    public static final Set<PermissionCode> ROLE_ALL = Set.of(
            ROLE_READ,
            ROLE_CREATE,
            ROLE_UPDATE,
            ROLE_DELETE
    );

    public static final Set<PermissionCode> USER_ALL = Set.of(
            USER_READ,
            USER_CREATE,
            USER_UPDATE,
            USER_DELETE
    );

    private IamPermissionCatalog() {
    }
}
