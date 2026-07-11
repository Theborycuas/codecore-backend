package com.codecore.patient.domain.valueobject;

import com.codecore.patient.domain.exception.InvalidDomainValueException;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Optional date of birth of the care subject. Must not be in the future.
 */
public final class DateOfBirth {

    private final LocalDate value;

    private DateOfBirth(LocalDate value) {
        this.value = value;
    }

    public static DateOfBirth of(LocalDate raw) {
        Objects.requireNonNull(raw, "raw");
        if (raw.isAfter(LocalDate.now())) {
            throw new InvalidDomainValueException("Date of birth cannot be in the future");
        }
        return new DateOfBirth(raw);
    }

    public LocalDate value() {
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
        DateOfBirth that = (DateOfBirth) other;
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
