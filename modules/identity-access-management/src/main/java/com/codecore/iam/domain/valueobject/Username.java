package com.codecore.iam.domain.valueobject;

import com.codecore.iam.domain.exception.InvalidDomainValueException;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Username {

    private static final Pattern FORMAT = Pattern.compile("^[a-zA-Z0-9._-]{3,64}$");

    private final String value;

    private Username(String value) {
        this.value = value;
    }

    public static Username of(String raw) {
        Objects.requireNonNull(raw, "raw");
        String normalized = raw.trim();
        if (!FORMAT.matcher(normalized).matches()) {
            throw new InvalidDomainValueException("Invalid username");
        }
        return new Username(normalized);
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
        Username that = (Username) other;
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
