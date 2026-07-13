package com.codecore.access.domain.valueobject;

import com.codecore.access.domain.exception.InvalidDomainValueException;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * System role code granted on accept (ADR-019) — allow-list only.
 * {@code OWNER} is rejected; OWNER is reserved for tenant bootstrap / exceptional IAM paths.
 */
public final class InvitationRoleCode {

    private static final Set<String> ALLOWED = Set.of("ADMIN", "MANAGER", "USER", "READ_ONLY");

    private final String value;

    private InvitationRoleCode(String value) {
        this.value = value;
    }

    public static InvitationRoleCode of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank() || !ALLOWED.contains(normalized)) {
            throw new InvalidDomainValueException("Invalid invitation role code: " + normalized);
        }
        return new InvitationRoleCode(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        InvitationRoleCode that = (InvitationRoleCode) other;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
