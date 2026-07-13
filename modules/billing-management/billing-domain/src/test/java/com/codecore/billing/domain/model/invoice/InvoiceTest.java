package com.codecore.billing.domain.model.invoice;

import com.codecore.billing.domain.exception.InvalidDomainValueException;
import com.codecore.billing.domain.exception.InvalidInvoiceStateException;
import com.codecore.billing.domain.valueobject.BillTo;
import com.codecore.billing.domain.valueobject.BillToOrganizationId;
import com.codecore.billing.domain.valueobject.BillToPatientId;
import com.codecore.billing.domain.valueobject.EncounterId;
import com.codecore.billing.domain.valueobject.InvoiceId;
import com.codecore.billing.domain.valueobject.InvoiceLineId;
import com.codecore.billing.domain.valueobject.InvoiceNumber;
import com.codecore.billing.domain.valueobject.InvoiceStatus;
import com.codecore.billing.domain.valueobject.ItemId;
import com.codecore.billing.domain.valueobject.LineDescription;
import com.codecore.billing.domain.valueobject.Money;
import com.codecore.billing.domain.valueobject.OrganizationId;
import com.codecore.billing.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceTest {

    private static final Instant NOW = Instant.parse("2026-07-12T18:00:00Z");

    @Test
    void shouldCreateDraftInvoiceWithPatientBillTo() {
        InvoiceId id = InvoiceId.generate();
        TenantId tenantId = TenantId.generate();
        OrganizationId issuer = OrganizationId.of(UUID.randomUUID());
        BillTo billTo = BillTo.patient(BillToPatientId.of(UUID.randomUUID()));
        InvoiceLine line = line("Consultation fee", 15000, null, null);

        Invoice invoice = Invoice.create(id, tenantId, issuer, billTo, null, List.of(line), NOW);

        assertThat(invoice.id()).isEqualTo(id);
        assertThat(invoice.tenantId()).isEqualTo(tenantId);
        assertThat(invoice.issuerOrganizationId()).isEqualTo(issuer);
        assertThat(invoice.billTo()).isEqualTo(billTo);
        assertThat(invoice.invoiceNumber()).isEmpty();
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.lines()).hasSize(1);
        assertThat(invoice.currency()).isEqualTo("USD");
        assertThat(invoice.totalAmountMinor()).isEqualTo(15000);
        assertThat(invoice.createdAt()).isEqualTo(NOW);
        assertThat(invoice.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldCreateDraftInvoiceWithOrganizationBillTo() {
        BillTo billTo = BillTo.organization(BillToOrganizationId.of(UUID.randomUUID()));

        Invoice invoice = Invoice.create(
                InvoiceId.generate(),
                TenantId.generate(),
                OrganizationId.of(UUID.randomUUID()),
                billTo,
                InvoiceNumber.of("INV-0001"),
                List.of(line("Retail goods", 5000, null, null)),
                NOW
        );

        assertThat(invoice.billTo().isOrganization()).isTrue();
        assertThat(invoice.invoiceNumber()).contains(InvoiceNumber.of("INV-0001"));
    }

    @Test
    void shouldRejectBillToOrganizationEqualToIssuer() {
        UUID sameOrg = UUID.randomUUID();

        assertThatThrownBy(() -> Invoice.create(
                InvoiceId.generate(),
                TenantId.generate(),
                OrganizationId.of(sameOrg),
                BillTo.organization(BillToOrganizationId.of(sameOrg)),
                null,
                List.of(line("Self-billed", 100, null, null)),
                NOW
        )).isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("must differ");
    }

    @Test
    void shouldRequireAtLeastOneLine() {
        assertThatThrownBy(() -> Invoice.create(
                InvoiceId.generate(),
                TenantId.generate(),
                OrganizationId.of(UUID.randomUUID()),
                BillTo.patient(BillToPatientId.of(UUID.randomUUID())),
                null,
                List.of(),
                NOW
        )).isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("at least one line");
    }

    @Test
    void shouldRejectMixedCurrencyLines() {
        InvoiceLine usdLine = line("USD line", 100, null, null);
        InvoiceLine eurLine = InvoiceLine.create(
                InvoiceLineId.generate(),
                LineDescription.of("EUR line"),
                Money.of("EUR", 100),
                null,
                null
        );

        assertThatThrownBy(() -> Invoice.create(
                InvoiceId.generate(),
                TenantId.generate(),
                OrganizationId.of(UUID.randomUUID()),
                BillTo.patient(BillToPatientId.of(UUID.randomUUID())),
                null,
                List.of(usdLine, eurLine),
                NOW
        )).isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("same currency");
    }

    @Test
    void shouldComputeTotalAsSumOfLines() {
        Invoice invoice = draftInvoice(List.of(
                line("Line A", 1000, null, null),
                line("Line B", 2500, null, null)
        ));

        assertThat(invoice.totalAmountMinor()).isEqualTo(3500);
    }

    @Test
    void shouldUpdateContentWhenDraft() {
        Invoice invoice = draftInvoice(List.of(line("Original", 1000, null, null)));
        Instant original = invoice.updatedAt();

        OrganizationId newIssuer = OrganizationId.of(UUID.randomUUID());
        BillTo newBillTo = BillTo.organization(BillToOrganizationId.of(UUID.randomUUID()));
        InvoiceLine newLine = line("Updated", 4200, ItemId.of(UUID.randomUUID()), null);

        invoice.updateContent(newIssuer, newBillTo, InvoiceNumber.of("INV-9"), List.of(newLine));

        assertThat(invoice.issuerOrganizationId()).isEqualTo(newIssuer);
        assertThat(invoice.billTo()).isEqualTo(newBillTo);
        assertThat(invoice.invoiceNumber()).contains(InvoiceNumber.of("INV-9"));
        assertThat(invoice.lines()).hasSize(1);
        assertThat(invoice.totalAmountMinor()).isEqualTo(4200);
        assertThat(invoice.updatedAt()).isAfterOrEqualTo(original);
    }

    @Test
    void shouldRejectUpdateContentWhenNotDraft() {
        Invoice invoice = draftInvoice(List.of(line("Original", 1000, null, null)));
        invoice.issue();

        assertThatThrownBy(() -> invoice.updateContent(
                invoice.issuerOrganizationId(),
                invoice.billTo(),
                null,
                List.of(line("New", 100, null, null))
        )).isInstanceOf(InvalidInvoiceStateException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void shouldIssueFromDraft() {
        Invoice invoice = draftInvoice(List.of(line("Line", 1000, null, null)));

        invoice.issue();

        assertThat(invoice.status()).isEqualTo(InvoiceStatus.ISSUED);
    }

    @Test
    void shouldRejectIssueWhenNotDraft() {
        Invoice invoice = draftInvoice(List.of(line("Line", 1000, null, null)));
        invoice.issue();

        assertThatThrownBy(invoice::issue)
                .isInstanceOf(InvalidInvoiceStateException.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void shouldVoidFromDraft() {
        Invoice invoice = draftInvoice(List.of(line("Line", 1000, null, null)));

        invoice.voidInvoice();

        assertThat(invoice.status()).isEqualTo(InvoiceStatus.VOIDED);
    }

    @Test
    void shouldVoidFromIssued() {
        Invoice invoice = draftInvoice(List.of(line("Line", 1000, null, null)));
        invoice.issue();

        invoice.voidInvoice();

        assertThat(invoice.status()).isEqualTo(InvoiceStatus.VOIDED);
    }

    @Test
    void shouldRejectVoidWhenAlreadyVoided() {
        Invoice invoice = draftInvoice(List.of(line("Line", 1000, null, null)));
        invoice.voidInvoice();

        assertThatThrownBy(invoice::voidInvoice)
                .isInstanceOf(InvalidInvoiceStateException.class)
                .hasMessageContaining("already voided");
    }

    @Test
    void shouldKeepTenantIdImmutableAfterMutations() {
        Invoice invoice = draftInvoice(List.of(line("Line", 1000, null, null)));
        TenantId original = invoice.tenantId();

        invoice.updateContent(
                OrganizationId.of(UUID.randomUUID()),
                BillTo.patient(BillToPatientId.of(UUID.randomUUID())),
                null,
                List.of(line("New", 500, null, null))
        );
        invoice.issue();
        invoice.voidInvoice();

        assertThat(invoice.tenantId()).isEqualTo(original);
    }

    @Test
    void shouldReconstituteInvoice() {
        InvoiceId id = InvoiceId.generate();
        TenantId tenantId = TenantId.generate();
        OrganizationId issuer = OrganizationId.of(UUID.randomUUID());
        BillTo billTo = BillTo.patient(BillToPatientId.of(UUID.randomUUID()));
        InvoiceLine invoiceLine = InvoiceLine.reconstitute(
                InvoiceLineId.generate(),
                LineDescription.of("Historic line"),
                Money.of("USD", 999),
                ItemId.of(UUID.randomUUID()),
                EncounterId.of(UUID.randomUUID())
        );
        Instant createdAt = NOW.minusSeconds(3600);
        Instant updatedAt = NOW.minusSeconds(60);

        Invoice invoice = Invoice.reconstitute(
                id,
                tenantId,
                issuer,
                billTo,
                InvoiceNumber.of("OLD-1"),
                List.of(invoiceLine),
                InvoiceStatus.ISSUED,
                createdAt,
                updatedAt
        );

        assertThat(invoice.id()).isEqualTo(id);
        assertThat(invoice.status()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(invoice.lines()).hasSize(1);
        assertThat(invoice.lines().get(0).itemId()).contains(ItemId.of(invoiceLine.itemId().orElseThrow().value()));
        assertThat(invoice.createdAt()).isEqualTo(createdAt);
        assertThat(invoice.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldNeverExposePaymentsTaxLedgerStockOrSubscriptionConcernsInPublicApi() {
        assertThat(Invoice.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain(
                        "addPayment",
                        "recordPayment",
                        "pay",
                        "calculateTax",
                        "applyTax",
                        "deductStock",
                        "adjustStock",
                        "addSubscription",
                        "assignPlan",
                        "postToLedger",
                        "post",
                        "toJournalEntry"
                );
    }

    private static Invoice draftInvoice(List<InvoiceLine> lines) {
        return Invoice.create(
                InvoiceId.generate(),
                TenantId.generate(),
                OrganizationId.of(UUID.randomUUID()),
                BillTo.patient(BillToPatientId.of(UUID.randomUUID())),
                null,
                lines,
                NOW
        );
    }

    private static InvoiceLine line(String description, long amountMinor, ItemId itemId, EncounterId encounterId) {
        return InvoiceLine.create(
                InvoiceLineId.generate(),
                LineDescription.of(description),
                Money.of("USD", amountMinor),
                itemId,
                encounterId
        );
    }
}
