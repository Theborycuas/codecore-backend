package com.codecore.billing.interfaces.http.admin.dto;

import com.codecore.billing.application.dto.AdminInvoiceLineView;

import java.util.UUID;

public record InvoiceLineResponse(
        UUID id,
        String description,
        long amountMinor,
        String currency,
        UUID itemId,
        UUID encounterId
) {

    public static InvoiceLineResponse from(AdminInvoiceLineView view) {
        return new InvoiceLineResponse(
                view.id().value(),
                view.description(),
                view.amountMinor(),
                view.currency(),
                view.itemUuid(),
                view.encounterUuid()
        );
    }
}
