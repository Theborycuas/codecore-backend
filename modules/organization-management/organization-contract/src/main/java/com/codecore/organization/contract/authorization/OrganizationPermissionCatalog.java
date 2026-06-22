package com.codecore.organization.contract.authorization;

import java.util.Set;

/**
 * Organization Management permission contract (FASE 16.3).
 * <p>
 * String codes are the canonical identifiers seeded in {@code iam.permission}.
 * IAM maps them to {@code PermissionCode} via {@code IamPermissionCatalog}.
 */
public final class OrganizationPermissionCatalog {

    public static final String ORGANIZATION_CREATE = "organization:create";
    public static final String ORGANIZATION_READ = "organization:read";
    public static final String ORGANIZATION_UPDATE = "organization:update";
    public static final String ORGANIZATION_ARCHIVE = "organization:archive";

    public static final String OFFICE_CREATE = "office:create";
    public static final String OFFICE_READ = "office:read";
    public static final String OFFICE_UPDATE = "office:update";
    public static final String OFFICE_ARCHIVE = "office:archive";

    public static final String STAFF_ASSIGNMENT_CREATE = "staff-assignment:create";
    public static final String STAFF_ASSIGNMENT_READ = "staff-assignment:read";
    public static final String STAFF_ASSIGNMENT_UPDATE = "staff-assignment:update";
    public static final String STAFF_ASSIGNMENT_DELETE = "staff-assignment:delete";

    public static final Set<String> ORGANIZATION_ALL = Set.of(
            ORGANIZATION_CREATE,
            ORGANIZATION_READ,
            ORGANIZATION_UPDATE,
            ORGANIZATION_ARCHIVE
    );

    public static final Set<String> OFFICE_ALL = Set.of(
            OFFICE_CREATE,
            OFFICE_READ,
            OFFICE_UPDATE,
            OFFICE_ARCHIVE
    );

    public static final Set<String> STAFF_ASSIGNMENT_ALL = Set.of(
            STAFF_ASSIGNMENT_CREATE,
            STAFF_ASSIGNMENT_READ,
            STAFF_ASSIGNMENT_UPDATE,
            STAFF_ASSIGNMENT_DELETE
    );

    /** Read-only navigation of tenant org structure (Organization, Office, StaffAssignment). */
    public static final Set<String> STRUCTURE_READ = Set.of(
            ORGANIZATION_READ,
            OFFICE_READ,
            STAFF_ASSIGNMENT_READ
    );

    /** Full Organization Management platform contract (FASE 16 — no clinical/billing modules). */
    public static final Set<String> ALL = Set.of(
            ORGANIZATION_CREATE,
            ORGANIZATION_READ,
            ORGANIZATION_UPDATE,
            ORGANIZATION_ARCHIVE,
            OFFICE_CREATE,
            OFFICE_READ,
            OFFICE_UPDATE,
            OFFICE_ARCHIVE,
            STAFF_ASSIGNMENT_CREATE,
            STAFF_ASSIGNMENT_READ,
            STAFF_ASSIGNMENT_UPDATE,
            STAFF_ASSIGNMENT_DELETE
    );

    private OrganizationPermissionCatalog() {
    }
}
