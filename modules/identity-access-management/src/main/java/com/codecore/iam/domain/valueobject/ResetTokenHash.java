package com.codecore.iam.domain.valueobject;

import java.util.Objects;

public final class ResetTokenHash {

    private final String value;

    private ResetTokenHash(String value) {
        this.value = value;
    }

    public static ResetTokenHash ofHashedValue(String hashedValue) {
        Objects.requireNonNull(hashedValue, "hashedValue");
        String normalized = hashedValue.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Reset token hash must not be blank");
        }
        return new ResetTokenHash(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "[protected]";
    }
}
