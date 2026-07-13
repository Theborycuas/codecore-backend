package com.codecore.audit.domain.valueobject;

import com.codecore.audit.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Opaque action code for an audit entry (ADR-020) — e.g. {@code invitation.created}.
 * Not free text; capped at 64 chars; must not be blank.
 */
public final class ActionCode {

    private static final int MAX_LENGTH = 64;

    private final String value;

    private ActionCode(String value) {
        this.value = value;
    }

    public static ActionCode of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isBlank()) {
            throw new InvalidDomainValueException("actionCode must not be blank");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("actionCode must be <= " + MAX_LENGTH + " characters");
        }
        return new ActionCode(trimmed);
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
        ActionCode that = (ActionCode) other;
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
