package com.codecore.billing.domain.model.invoice;

import com.codecore.billing.domain.valueobject.EncounterId;
import com.codecore.billing.domain.valueobject.InvoiceLineId;
import com.codecore.billing.domain.valueobject.ItemId;
import com.codecore.billing.domain.valueobject.LineDescription;
import com.codecore.billing.domain.valueobject.Money;

import java.util.Objects;
import java.util.Optional;

/**
 * InvoiceLine — internal entity of {@link Invoice} (ADR-017 §8).
 * <p>
 * Intentionally minimal: stable id, non-blank description, resolved {@link Money}, optional
 * {@link ItemId} (material line), optional {@link EncounterId} (clinical origin line).
 * No quantity, unit-of-measure, unit price, tax breakdown, StockId, or AppointmentId.
 * Immutable — {@link Invoice} replaces the whole line list on content update.
 */
public final class InvoiceLine {

    private final InvoiceLineId id;
    private final LineDescription description;
    private final Money amount;
    private final ItemId itemId;
    private final EncounterId encounterId;

    private InvoiceLine(
            InvoiceLineId id,
            LineDescription description,
            Money amount,
            ItemId itemId,
            EncounterId encounterId
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = Objects.requireNonNull(description, "description");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.itemId = itemId;
        this.encounterId = encounterId;
    }

    public static InvoiceLine create(
            InvoiceLineId id,
            LineDescription description,
            Money amount,
            ItemId itemId,
            EncounterId encounterId
    ) {
        return new InvoiceLine(id, description, amount, itemId, encounterId);
    }

    public static InvoiceLine reconstitute(
            InvoiceLineId id,
            LineDescription description,
            Money amount,
            ItemId itemId,
            EncounterId encounterId
    ) {
        return new InvoiceLine(id, description, amount, itemId, encounterId);
    }

    public InvoiceLineId id() {
        return id;
    }

    public LineDescription description() {
        return description;
    }

    public Money amount() {
        return amount;
    }

    public Optional<ItemId> itemId() {
        return Optional.ofNullable(itemId);
    }

    public Optional<EncounterId> encounterId() {
        return Optional.ofNullable(encounterId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        InvoiceLine that = (InvoiceLine) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
