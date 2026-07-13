package com.codecore.billing.infrastructure.adapters;

import com.codecore.billing.application.port.out.InvoiceRepository;
import com.codecore.billing.contract.reference.InvoiceReferencePort;
import com.codecore.billing.domain.model.invoice.Invoice;
import com.codecore.billing.domain.model.invoice.InvoiceLine;
import com.codecore.billing.domain.valueobject.BillTo;
import com.codecore.billing.domain.valueobject.BillToPatientId;
import com.codecore.billing.domain.valueobject.InvoiceId;
import com.codecore.billing.domain.valueobject.InvoiceLineId;
import com.codecore.billing.domain.valueobject.LineDescription;
import com.codecore.billing.domain.valueobject.Money;
import com.codecore.billing.domain.valueobject.OrganizationId;
import com.codecore.billing.domain.valueobject.TenantId;
import com.codecore.billing.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.billing.testsupport.InvoicePersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@DataR2dbcTest
@Import({
        InvoicePersistenceTestConfiguration.class,
        R2dbcInvoiceReferenceAdapter.class
})
class InvoiceReferencePortIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-12T21:00:00Z");

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceReferencePort invoiceReferencePort;

    @Test
    void shouldReturnTrueForIssuedInvoiceInTenant() {
        InvoiceId invoiceId = InvoiceId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(invoiceRepository.save(draftInvoice(invoiceId, tenantId)).flatMap(saved -> {
                    saved.issue();
                    return invoiceRepository.save(saved);
                }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invoiceReferencePort.existsIssuedByIdAndTenant(invoiceId, tenantId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseForWrongTenantOrUnknownId() {
        InvoiceId invoiceId = InvoiceId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(invoiceRepository.save(draftInvoice(invoiceId, tenantId)).flatMap(saved -> {
                    saved.issue();
                    return invoiceRepository.save(saved);
                }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invoiceReferencePort.existsIssuedByIdAndTenant(invoiceId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(invoiceReferencePort.existsIssuedByIdAndTenant(InvoiceId.generate(), tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseWhenInvoiceIsDraftOrVoided() {
        InvoiceId draftId = InvoiceId.generate();
        InvoiceId voidedId = InvoiceId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(invoiceRepository.save(draftInvoice(draftId, tenantId)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invoiceReferencePort.existsIssuedByIdAndTenant(draftId, tenantId))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(invoiceRepository.save(draftInvoice(voidedId, tenantId)).flatMap(saved -> {
                    saved.issue();
                    saved.voidInvoice();
                    return invoiceRepository.save(saved);
                }))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invoiceReferencePort.existsIssuedByIdAndTenant(voidedId, tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    private static Invoice draftInvoice(InvoiceId invoiceId, TenantId tenantId) {
        OrganizationId issuer = OrganizationId.of(UUID.randomUUID());
        BillTo billTo = BillTo.patient(BillToPatientId.of(UUID.randomUUID()));
        InvoiceLine line = InvoiceLine.create(
                InvoiceLineId.generate(),
                LineDescription.of("Reference port fixture line"),
                Money.of("USD", 10_000L),
                null,
                null
        );
        return Invoice.create(invoiceId, tenantId, issuer, billTo, null, List.of(line), NOW);
    }
}
