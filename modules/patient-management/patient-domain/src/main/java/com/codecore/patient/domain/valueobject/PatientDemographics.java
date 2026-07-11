package com.codecore.patient.domain.valueobject;

import java.util.Objects;
import java.util.Optional;

/**
 * Mutable-registry demographics of a care subject — intentionally small (ADR-012 §3).
 * Does not include clinical, operational, or vertical-profile fields.
 */
public final class PatientDemographics {

    private final PatientDisplayName displayName;
    private final ContactEmail email;
    private final ContactPhone phone;
    private final DateOfBirth dateOfBirth;

    private PatientDemographics(
            PatientDisplayName displayName,
            ContactEmail email,
            ContactPhone phone,
            DateOfBirth dateOfBirth
    ) {
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
    }

    public static PatientDemographics of(PatientDisplayName displayName) {
        return new PatientDemographics(displayName, null, null, null);
    }

    public static PatientDemographics of(
            PatientDisplayName displayName,
            ContactEmail email,
            ContactPhone phone,
            DateOfBirth dateOfBirth
    ) {
        return new PatientDemographics(displayName, email, phone, dateOfBirth);
    }

    public PatientDisplayName displayName() {
        return displayName;
    }

    public Optional<ContactEmail> email() {
        return Optional.ofNullable(email);
    }

    public Optional<ContactPhone> phone() {
        return Optional.ofNullable(phone);
    }

    public Optional<DateOfBirth> dateOfBirth() {
        return Optional.ofNullable(dateOfBirth);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        PatientDemographics that = (PatientDemographics) other;
        return displayName.equals(that.displayName)
                && Objects.equals(email, that.email)
                && Objects.equals(phone, that.phone)
                && Objects.equals(dateOfBirth, that.dateOfBirth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, email, phone, dateOfBirth);
    }
}
