package com.codecore.iam.application.admin;

import com.codecore.iam.domain.valueobject.RoleCode;

/**
 * System role privilege ordering (PASO 15.0.1).
 */
public enum RolePrivilegeLevel {

    READ_ONLY(1),
    USER(2),
    MANAGER(3),
    ADMIN(4),
    OWNER(5);

    private final int level;

    RolePrivilegeLevel(int level) {
        this.level = level;
    }

    public int level() {
        return level;
    }

    public static RolePrivilegeLevel fromRoleCode(RoleCode code) {
        return switch (code.value()) {
            case "OWNER" -> OWNER;
            case "ADMIN" -> ADMIN;
            case "MANAGER" -> MANAGER;
            case "USER" -> USER;
            case "READ_ONLY" -> READ_ONLY;
            default -> USER;
        };
    }
}
