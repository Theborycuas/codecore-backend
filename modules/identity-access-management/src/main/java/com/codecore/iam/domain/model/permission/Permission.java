package com.codecore.iam.domain.model.permission;

import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.PermissionId;

import java.time.Instant;
import java.util.Objects;

/**
 * Global permission aggregate root — atomic {@code resource:action} grant (not tenant-scoped).
 */
public final class Permission {

    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final PermissionId id;
    private final PermissionCode code;
    private String description;
    private final boolean systemPermission;
    private final Instant createdAt;
    private Instant updatedAt;

    public Permission(
            PermissionId id,
            PermissionCode code,
            String description,
            boolean systemPermission,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.code = Objects.requireNonNull(code, "code");
        this.description = normalizeDescription(description);
        this.systemPermission = systemPermission;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Permission create(PermissionCode code, String description, Instant now) {
        return create(code, description, false, now);
    }

    public static Permission createSystemPermission(PermissionCode code, String description, Instant now) {
        return create(code, description, true, now);
    }

    private static Permission create(
            PermissionCode code,
            String description,
            boolean systemPermission,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new Permission(
                PermissionId.generate(),
                code,
                description,
                systemPermission,
                now,
                now
        );
    }

    public PermissionId id() {
        return id;
    }

    public PermissionCode code() {
        return code;
    }

    public String description() {
        return description;
    }

    public boolean systemPermission() {
        return systemPermission;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void updateDescription(String newDescription) {
        ensureMutable();
        this.description = normalizeDescription(newDescription);
        touch();
    }

    private void ensureMutable() {
        if (systemPermission) {
            throw new IllegalStateException("System permissions cannot be modified");
        }
    }

    private static String normalizeDescription(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Permission description too long");
        }
        return trimmed;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
