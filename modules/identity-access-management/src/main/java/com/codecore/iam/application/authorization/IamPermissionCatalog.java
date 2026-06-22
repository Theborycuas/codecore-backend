package com.codecore.iam.application.authorization;

import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.organization.contract.authorization.OrganizationPermissionCatalog;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Platform permission contract: IAM foundation (FASE 14) + Organization Management (FASE 16.3).
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

    public static final PermissionCode ORGANIZATION_CREATE =
            PermissionCode.of(OrganizationPermissionCatalog.ORGANIZATION_CREATE);
    public static final PermissionCode ORGANIZATION_READ =
            PermissionCode.of(OrganizationPermissionCatalog.ORGANIZATION_READ);
    public static final PermissionCode ORGANIZATION_UPDATE =
            PermissionCode.of(OrganizationPermissionCatalog.ORGANIZATION_UPDATE);
    public static final PermissionCode ORGANIZATION_ARCHIVE =
            PermissionCode.of(OrganizationPermissionCatalog.ORGANIZATION_ARCHIVE);

    public static final PermissionCode OFFICE_CREATE =
            PermissionCode.of(OrganizationPermissionCatalog.OFFICE_CREATE);
    public static final PermissionCode OFFICE_READ =
            PermissionCode.of(OrganizationPermissionCatalog.OFFICE_READ);
    public static final PermissionCode OFFICE_UPDATE =
            PermissionCode.of(OrganizationPermissionCatalog.OFFICE_UPDATE);
    public static final PermissionCode OFFICE_ARCHIVE =
            PermissionCode.of(OrganizationPermissionCatalog.OFFICE_ARCHIVE);

    public static final PermissionCode STAFF_ASSIGNMENT_CREATE =
            PermissionCode.of(OrganizationPermissionCatalog.STAFF_ASSIGNMENT_CREATE);
    public static final PermissionCode STAFF_ASSIGNMENT_READ =
            PermissionCode.of(OrganizationPermissionCatalog.STAFF_ASSIGNMENT_READ);
    public static final PermissionCode STAFF_ASSIGNMENT_UPDATE =
            PermissionCode.of(OrganizationPermissionCatalog.STAFF_ASSIGNMENT_UPDATE);
    public static final PermissionCode STAFF_ASSIGNMENT_DELETE =
            PermissionCode.of(OrganizationPermissionCatalog.STAFF_ASSIGNMENT_DELETE);

    public static final Set<PermissionCode> IAM_FOUNDATION = Set.of(
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

    public static final Set<PermissionCode> ORGANIZATION_ALL = codesOf(OrganizationPermissionCatalog.ORGANIZATION_ALL);
    public static final Set<PermissionCode> OFFICE_ALL = codesOf(OrganizationPermissionCatalog.OFFICE_ALL);
    public static final Set<PermissionCode> STAFF_ASSIGNMENT_ALL =
            codesOf(OrganizationPermissionCatalog.STAFF_ASSIGNMENT_ALL);
    public static final Set<PermissionCode> STRUCTURE_READ = codesOf(OrganizationPermissionCatalog.STRUCTURE_READ);
    public static final Set<PermissionCode> ORGANIZATION_PLATFORM_ALL =
            codesOf(OrganizationPermissionCatalog.ALL);

    public static final Set<PermissionCode> ALL = union(IAM_FOUNDATION, ORGANIZATION_PLATFORM_ALL);

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

    /** ADMIN IAM grants (FASE 14.8) — excludes tenant governance and permission catalog read. */
    public static final Set<PermissionCode> ADMIN_IAM = union(
            MEMBERSHIP_ALL,
            ROLE_ALL,
            Set.of(PERMISSION_ASSIGN),
            USER_ALL
    );

    /** MANAGER operational IAM grants. */
    public static final Set<PermissionCode> MANAGER_IAM = Set.of(
            MEMBERSHIP_READ,
            USER_READ,
            USER_UPDATE
    );

    /** MANAGER Organization Management grants — offices + staff; org read-only. */
    public static final Set<PermissionCode> MANAGER_ORGANIZATION = union(
            Set.of(ORGANIZATION_READ),
            OFFICE_ALL,
            STAFF_ASSIGNMENT_ALL
    );

    private IamPermissionCatalog() {
    }

    private static Set<PermissionCode> codesOf(Set<String> rawCodes) {
        return rawCodes.stream().map(PermissionCode::of).collect(Collectors.toUnmodifiableSet());
    }

    static Set<PermissionCode> union(Set<PermissionCode> first, Set<PermissionCode> second) {
        Set<PermissionCode> combined = new HashSet<>(first);
        combined.addAll(second);
        return Set.copyOf(combined);
    }

    @SafeVarargs
    static Set<PermissionCode> union(Set<PermissionCode>... sets) {
        Set<PermissionCode> combined = new HashSet<>();
        for (Set<PermissionCode> set : sets) {
            combined.addAll(set);
        }
        return Set.copyOf(combined);
    }
}
