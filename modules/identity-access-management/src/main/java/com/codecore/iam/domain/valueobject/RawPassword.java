package com.codecore.iam.domain.valueobject;

import com.codecore.iam.domain.exception.InvalidDomainValueException;

import java.util.Objects;

/**
 * Transient credential input before hashing. Must never be persisted or logged.
 * Enterprise password rules are enforced via {@link com.codecore.iam.application.port.out.PasswordPolicyPort}.
 */
public final class RawPassword {

    private static final int MIN_LENGTH = 8;

    private final String value;

    private RawPassword(String value) {
        this.value = value;
    }

    /**
     * Domain guard: rejects blank or trivially short secrets. Full policy belongs to application layer.
     */
    public static RawPassword of(String raw) {
        Objects.requireNonNull(raw, "raw");
        if (raw.length() < MIN_LENGTH) {
            throw new InvalidDomainValueException("Password is too short");
        }
        return new RawPassword(raw);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "[protected]";
    }
}
