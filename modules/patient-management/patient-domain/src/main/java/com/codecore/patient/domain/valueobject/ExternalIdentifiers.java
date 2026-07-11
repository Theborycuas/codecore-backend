package com.codecore.patient.domain.valueobject;

import com.codecore.patient.domain.exception.InvalidDomainValueException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Small set of typed external identifiers on a Patient registry entry.
 * At most one value per {@link ExternalIdentifierType}.
 */
public final class ExternalIdentifiers {

    private final Map<ExternalIdentifierType, ExternalIdentifier> byType;

    private ExternalIdentifiers(Map<ExternalIdentifierType, ExternalIdentifier> byType) {
        this.byType = Map.copyOf(byType);
    }

    public static ExternalIdentifiers empty() {
        return new ExternalIdentifiers(Map.of());
    }

    public static ExternalIdentifiers of(Collection<ExternalIdentifier> identifiers) {
        Objects.requireNonNull(identifiers, "identifiers");
        Map<ExternalIdentifierType, ExternalIdentifier> map = new LinkedHashMap<>();
        for (ExternalIdentifier identifier : identifiers) {
            Objects.requireNonNull(identifier, "identifier");
            if (map.containsKey(identifier.type())) {
                throw new InvalidDomainValueException(
                        "Duplicate external identifier type: " + identifier.type().value()
                );
            }
            map.put(identifier.type(), identifier);
        }
        return new ExternalIdentifiers(map);
    }

    public Set<ExternalIdentifier> asSet() {
        return Set.copyOf(byType.values());
    }

    public Optional<ExternalIdentifier> find(ExternalIdentifierType type) {
        return Optional.ofNullable(byType.get(type));
    }

    public boolean isEmpty() {
        return byType.isEmpty();
    }

    public int size() {
        return byType.size();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        ExternalIdentifiers that = (ExternalIdentifiers) other;
        return byType.equals(that.byType);
    }

    @Override
    public int hashCode() {
        return byType.hashCode();
    }

    @Override
    public String toString() {
        return byType.values().toString();
    }
}
