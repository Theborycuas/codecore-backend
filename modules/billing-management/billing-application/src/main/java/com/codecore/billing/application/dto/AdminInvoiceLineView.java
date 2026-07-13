package com.codecore.billing.application.dto;

import com.codecore.billing.domain.valueobject.EncounterId;
import com.codecore.billing.domain.valueobject.InvoiceLineId;
import com.codecore.billing.domain.valueobject.ItemId;

import java.util.UUID;

public record AdminInvoiceLineView(
        InvoiceLineId id,
        String description,
        long amountMinor,
        String currency,
        ItemId itemId,
        EncounterId encounterId
) {

    public UUID itemUuid() {
        return itemId == null ? null : itemId.value();
    }

    public UUID encounterUuid() {
        return encounterId == null ? null : encounterId.value();
    }
}
