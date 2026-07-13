package com.codecore.billing.application.command;

import java.util.UUID;

/**
 * Input shape for a single Invoice line — amount resolved in the Invoice's currency.
 */
public record InvoiceLineDraft(
        String description,
        long amountMinor,
        UUID itemId,
        UUID encounterId
) {
}
