package com.codecore.billing.domain.model.invoice;

import com.codecore.billing.domain.exception.InvalidDomainValueException;
import com.codecore.billing.domain.exception.InvalidInvoiceStateException;
import com.codecore.billing.domain.valueobject.BillTo;
import com.codecore.billing.domain.valueobject.InvoiceId;
import com.codecore.billing.domain.valueobject.InvoiceNumber;
import com.codecore.billing.domain.valueobject.InvoiceStatus;
import com.codecore.billing.domain.valueobject.OrganizationId;
import com.codecore.billing.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Invoice aggregate root — the commercial claim that an amount is owed by a bill-to party to an
 * issuing organization within a Tenant (ADR-017).
 * <p>
 * <strong>One sentence:</strong> the commercial claim of an amount owed under a Tenant.
 * <p>
 * Intentionally small: issuer + bill-to (Patient xor Organization) + optional human number +
 * internal lines + soft lifecycle DRAFT/ISSUED/VOIDED. Never embeds payments, tax breakdown,
 * general ledger entries, stock deductions, or subscriptions.
 */
public final class Invoice {

    private final InvoiceId id;
    private final TenantId tenantId;
    private OrganizationId issuerOrganizationId;
    private BillTo billTo;
    private InvoiceNumber invoiceNumber;
    private List<InvoiceLine> lines;
    private InvoiceStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Invoice(
            InvoiceId id,
            TenantId tenantId,
            OrganizationId issuerOrganizationId,
            BillTo billTo,
            InvoiceNumber invoiceNumber,
            List<InvoiceLine> lines,
            InvoiceStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.issuerOrganizationId = Objects.requireNonNull(issuerOrganizationId, "issuerOrganizationId");
        this.billTo = Objects.requireNonNull(billTo, "billTo");
        this.invoiceNumber = invoiceNumber;
        this.lines = new ArrayList<>(Objects.requireNonNull(lines, "lines"));
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Invoice create(
            InvoiceId id,
            TenantId tenantId,
            OrganizationId issuerOrganizationId,
            BillTo billTo,
            InvoiceNumber invoiceNumber,
            List<InvoiceLine> lines,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        Invoice invoice = new Invoice(
                id,
                tenantId,
                issuerOrganizationId,
                billTo,
                invoiceNumber,
                lines,
                InvoiceStatus.DRAFT,
                now,
                now
        );
        invoice.validateInvariants();
        return invoice;
    }

    public static Invoice reconstitute(
            InvoiceId id,
            TenantId tenantId,
            OrganizationId issuerOrganizationId,
            BillTo billTo,
            InvoiceNumber invoiceNumber,
            List<InvoiceLine> lines,
            InvoiceStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Invoice(
                id,
                tenantId,
                issuerOrganizationId,
                billTo,
                invoiceNumber,
                lines,
                status,
                createdAt,
                updatedAt
        );
    }

    public InvoiceId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public OrganizationId issuerOrganizationId() {
        return issuerOrganizationId;
    }

    public BillTo billTo() {
        return billTo;
    }

    public Optional<InvoiceNumber> invoiceNumber() {
        return Optional.ofNullable(invoiceNumber);
    }

    public List<InvoiceLine> lines() {
        return List.copyOf(lines);
    }

    /** Single currency across all lines (ADR-017 §6) — always present once the invariants hold. */
    public String currency() {
        return lines.get(0).amount().currency();
    }

    /** Total = sum of line amountMinors; always > 0 given the >= 1 line + amountMinor > 0 invariants. */
    public long totalAmountMinor() {
        return lines.stream().mapToLong(line -> line.amount().amountMinor()).sum();
    }

    public InvoiceStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    /**
     * Full replace of mutable content (PUT semantics) — only allowed in {@code DRAFT} (ADR-017 §9).
     */
    public void updateContent(
            OrganizationId issuerOrganizationId,
            BillTo billTo,
            InvoiceNumber invoiceNumber,
            List<InvoiceLine> lines
    ) {
        requireDraft("update content");
        this.issuerOrganizationId = Objects.requireNonNull(issuerOrganizationId, "issuerOrganizationId");
        this.billTo = Objects.requireNonNull(billTo, "billTo");
        this.invoiceNumber = invoiceNumber;
        this.lines = new ArrayList<>(Objects.requireNonNull(lines, "lines"));
        validateInvariants();
        touch();
    }

    /** {@code DRAFT -> ISSUED}. Re-validates content invariants (ReferencePorts are re-checked by the caller). */
    public void issue() {
        if (status != InvoiceStatus.DRAFT) {
            throw new InvalidInvoiceStateException("Only DRAFT invoices can be issued");
        }
        validateInvariants();
        this.status = InvoiceStatus.ISSUED;
        touch();
    }

    /** {@code DRAFT|ISSUED -> VOIDED}. No physical delete; no un-void; no ReferencePort re-validation. */
    public void voidInvoice() {
        if (status == InvoiceStatus.VOIDED) {
            throw new InvalidInvoiceStateException("Invoice is already voided");
        }
        this.status = InvoiceStatus.VOIDED;
        touch();
    }

    private void requireDraft(String action) {
        if (status != InvoiceStatus.DRAFT) {
            throw new InvalidInvoiceStateException("Cannot " + action + " when invoice is not DRAFT");
        }
    }

    private void validateInvariants() {
        if (billTo.isOrganization()
                && billTo.organizationId().orElseThrow().value().equals(issuerOrganizationId.value())) {
            throw new InvalidDomainValueException("Bill-to organization must differ from issuer organization");
        }
        if (lines.isEmpty()) {
            throw new InvalidDomainValueException("Invoice must have at least one line");
        }
        String expectedCurrency = lines.get(0).amount().currency();
        for (InvoiceLine line : lines) {
            if (!line.amount().currency().equals(expectedCurrency)) {
                throw new InvalidDomainValueException("All invoice lines must share the same currency");
            }
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
