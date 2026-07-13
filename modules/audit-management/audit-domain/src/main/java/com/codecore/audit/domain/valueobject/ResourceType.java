package com.codecore.audit.domain.valueobject;

import com.codecore.audit.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Opaque resource type label for an audit entry (ADR-020) — e.g. {@code invitation}.
 * Capped at 64 chars; must not be blank. Not validated against a catalog.
 */
public final class ResourceType {

    private static final int MAX_LENGTH = 64;

    private final String value;

    private ResourceType(String value) {
        this.value = value;
    }

    public static ResourceType of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isBlank()) {
            throw new InvalidDomainValueException("resourceType must not be blank");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidDomainValueException("resourceType must be <= " + MAX_LENGTH + " characters");
        }
        return new ResourceType(trimmed);
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
        ResourceType that = (ResourceType) other;
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
