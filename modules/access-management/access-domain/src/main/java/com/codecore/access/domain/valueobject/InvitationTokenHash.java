package com.codecore.access.domain.valueobject;

import com.codecore.access.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Already-hashed invitation token value (ADR-019). Never stores the raw token.
 */
public final class InvitationTokenHash {

    private final String value;

    private InvitationTokenHash(String value) {
        this.value = value;
    }

    public static InvitationTokenHash ofHashedValue(String hashedValue) {
        Objects.requireNonNull(hashedValue, "hashedValue");
        String normalized = hashedValue.trim();
        if (normalized.isBlank()) {
            throw new InvalidDomainValueException("Invitation token hash must not be blank");
        }
        return new InvitationTokenHash(normalized);
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
        InvitationTokenHash that = (InvitationTokenHash) other;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "[protected]";
    }
}
