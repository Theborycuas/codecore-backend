package com.codecore.payment.domain.valueobject;

import com.codecore.payment.domain.exception.InvalidDomainValueException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Already-resolved monetary amount — ISO 4217 {@code currency} + {@code amountMinor} minor units
 * (ADR-018). No FX engine, no tax breakdown; amount must be strictly positive.
 */
public final class Money {

    private static final Pattern ISO_4217 = Pattern.compile("^[A-Z]{3}$");

    private final String currency;
    private final long amountMinor;

    private Money(String currency, long amountMinor) {
        this.currency = currency;
        this.amountMinor = amountMinor;
    }

    public static Money of(String currency, long amountMinor) {
        Objects.requireNonNull(currency, "currency");
        String normalized = currency.trim().toUpperCase();
        if (!ISO_4217.matcher(normalized).matches()) {
            throw new InvalidDomainValueException("Invalid ISO 4217 currency code");
        }
        if (amountMinor <= 0) {
            throw new InvalidDomainValueException("Money amountMinor must be > 0");
        }
        return new Money(normalized, amountMinor);
    }

    public String currency() {
        return currency;
    }

    public long amountMinor() {
        return amountMinor;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Money that = (Money) other;
        return amountMinor == that.amountMinor && currency.equals(that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, amountMinor);
    }

    @Override
    public String toString() {
        return currency + " " + amountMinor;
    }
}
