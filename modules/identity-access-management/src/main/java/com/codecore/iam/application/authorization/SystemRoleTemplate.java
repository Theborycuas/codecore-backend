package com.codecore.iam.application.authorization;

import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleName;

import java.util.Set;

import static com.codecore.iam.application.authorization.IamPermissionCatalog.ADMIN_IAM;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.APPOINTMENT_PLATFORM_ALL;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.APPOINTMENT_READ_ONLY;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.ENCOUNTER_PLATFORM_ALL;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.ENCOUNTER_READ_ONLY;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.INVOICE_PLATFORM_ALL;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.INVOICE_READ_ONLY;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.ITEM_PLATFORM_ALL;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.ITEM_READ_ONLY;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.MANAGER_APPOINTMENT;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.MANAGER_ENCOUNTER;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.MANAGER_IAM;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.MANAGER_INVOICE;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.MANAGER_ITEM;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.MANAGER_ORGANIZATION;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.MANAGER_PATIENT;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.ORGANIZATION_PLATFORM_ALL;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.PATIENT_PLATFORM_ALL;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.PATIENT_READ_ONLY;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.STRUCTURE_READ;
import static com.codecore.iam.application.authorization.IamPermissionCatalog.union;

/**
 * Tenant-scoped system role templates
 * (FASE 14.8 + 16.3 org + 17.5 patient + 18.5 appointment + 19.5 encounter + 20.5 item + 21.5 invoice).
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
            union(
                    ADMIN_IAM,
                    ORGANIZATION_PLATFORM_ALL,
                    PATIENT_PLATFORM_ALL,
                    APPOINTMENT_PLATFORM_ALL,
                    ENCOUNTER_PLATFORM_ALL,
                    ITEM_PLATFORM_ALL,
                    INVOICE_PLATFORM_ALL
            )
    ),
    MANAGER(
            RoleCode.of("MANAGER"),
            RoleName.of("Manager"),
            union(
                    MANAGER_IAM,
                    MANAGER_ORGANIZATION,
                    MANAGER_PATIENT,
                    MANAGER_APPOINTMENT,
                    MANAGER_ENCOUNTER,
                    MANAGER_ITEM,
                    MANAGER_INVOICE
            )
    ),
    USER(
            RoleCode.of("USER"),
            RoleName.of("User"),
            union(
                    Set.of(IamPermissionCatalog.USER_READ),
                    STRUCTURE_READ,
                    PATIENT_READ_ONLY,
                    APPOINTMENT_READ_ONLY,
                    ENCOUNTER_READ_ONLY,
                    ITEM_READ_ONLY,
                    INVOICE_READ_ONLY
            )
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
                    STRUCTURE_READ,
                    PATIENT_READ_ONLY,
                    APPOINTMENT_READ_ONLY,
                    ENCOUNTER_READ_ONLY,
                    ITEM_READ_ONLY,
                    INVOICE_READ_ONLY
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
