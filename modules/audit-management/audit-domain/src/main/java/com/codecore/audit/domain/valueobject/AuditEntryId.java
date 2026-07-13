package com.codecore.audit.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * Surrogate identifier for {@link com.codecore.audit.domain.model.auditentry.AuditEntry}.
 * Hard unique identity of the audit entry aggregate (ADR-020).
 */
public final class AuditEntryId {

    private final UUID value;

    public AuditEntryId(UUID value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public AuditEntryId(String value) {
        this(UUID.fromString(Objects.requireNonNull(value, "value").trim()));
    }

    public static AuditEntryId of(UUID value) {
        return new AuditEntryId(value);
    }

    public static AuditEntryId generate() {
        return new AuditEntryId(UUID.randomUUID());
    }

    public UUID value() {
        return value;
    }

    public String asString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        AuditEntryId that = (AuditEntryId) other;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
