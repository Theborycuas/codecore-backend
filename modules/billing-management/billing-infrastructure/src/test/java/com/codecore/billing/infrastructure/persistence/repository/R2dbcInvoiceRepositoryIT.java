package com.codecore.billing.infrastructure.persistence.repository;

import com.codecore.billing.application.port.out.InvoiceQueryPort;
import com.codecore.billing.application.port.out.InvoiceRepository;
import com.codecore.billing.domain.model.invoice.Invoice;
import com.codecore.billing.domain.model.invoice.InvoiceLine;
import com.codecore.billing.domain.valueobject.BillTo;
import com.codecore.billing.domain.valueobject.BillToPatientId;
import com.codecore.billing.domain.valueobject.InvoiceId;
import com.codecore.billing.domain.valueobject.InvoiceLineId;
import com.codecore.billing.domain.valueobject.InvoiceNumber;
import com.codecore.billing.domain.valueobject.InvoiceStatus;
import com.codecore.billing.domain.valueobject.ItemId;
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
import org.springframework.dao.DuplicateKeyException;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(InvoicePersistenceTestConfiguration.class)
class R2dbcInvoiceRepositoryIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-12T19:00:00Z");

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceQueryPort invoiceQueryPort;

    @Test
    void shouldPersistAndFindByIdWithLines() {
        InvoiceId invoiceId = InvoiceId.generate();
        TenantId tenantId = TenantId.generate();
        Invoice invoice = draftInvoice(invoiceId, tenantId, null, twoLines());

        StepVerifier.create(invoiceRepository.save(invoice))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(invoiceId);
                    assertThat(saved.tenantId()).isEqualTo(tenantId);
                    assertThat(saved.lines()).hasSize(2);
                    assertThat(saved.totalAmountMinor()).isEqualTo(25000);
                    assertThat(saved.status()).isEqualTo(InvoiceStatus.DRAFT);
                })
                .verifyComplete();

        StepVerifier.create(invoiceRepository.findById(invoiceId))
                .assertNext(found -> {
                    assertThat(found.id()).isEqualTo(invoiceId);
                    assertThat(found.lines()).hasSize(2);
                    assertThat(found.totalAmountMinor()).isEqualTo(25000);
                })
                .verifyComplete();
    }

    @Test
    void shouldFullyReplaceLinesOnUpdate() {
        InvoiceId invoiceId = InvoiceId.generate();
        TenantId tenantId = TenantId.generate();
        Invoice invoice = draftInvoice(invoiceId, tenantId, null, twoLines());

        StepVerifier.create(invoiceRepository.save(invoice))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invoiceRepository.findById(invoiceId)
                        .flatMap(loaded -> {
                            loaded.updateContent(
                                    loaded.issuerOrganizationId(),
                                    loaded.billTo(),
                                    null,
                                    List.of(InvoiceLine.create(
                                            InvoiceLineId.generate(),
                                            LineDescription.of("Replaced single line"),
                                            Money.of("USD", 9900),
                                            null,
                                            null
                                    ))
                            );
                            return invoiceRepository.save(loaded);
                        }))
                .assertNext(saved -> {
                    assertThat(saved.lines()).hasSize(1);
                    assertThat(saved.totalAmountMinor()).isEqualTo(9900);
                })
                .verifyComplete();

        StepVerifier.create(invoiceRepository.findById(invoiceId))
                .assertNext(found -> assertThat(found.lines()).hasSize(1))
                .verifyComplete();
    }

    @Test
    void shouldReportExistsByIdAndTenant() {
        InvoiceId invoiceId = InvoiceId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(invoiceRepository.save(draftInvoice(invoiceId, tenantId, null, oneLine())))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invoiceRepository.existsById(invoiceId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(invoiceRepository.existsByIdAndTenantId(invoiceId, tenantId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(invoiceRepository.existsByIdAndTenantId(invoiceId, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldIsolateCrossTenantReads() {
        InvoiceId invoiceId = InvoiceId.generate();
        TenantId tenantId = TenantId.generate();
        TenantId otherTenantId = TenantId.generate();

        StepVerifier.create(invoiceRepository.save(draftInvoice(invoiceId, tenantId, null, oneLine())))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invoiceQueryPort.findByIdAndTenantId(invoiceId, tenantId))
                .assertNext(found -> assertThat(found.id()).isEqualTo(invoiceId))
                .verifyComplete();

        StepVerifier.create(invoiceQueryPort.findByIdAndTenantId(invoiceId, otherTenantId))
                .verifyComplete();
    }

    @Test
    void shouldCountAndFindByTenantIdAndStatus() {
        TenantId tenantId = TenantId.generate();
        InvoiceId draftId = InvoiceId.generate();
        InvoiceId issuedId = InvoiceId.generate();

        StepVerifier.create(invoiceRepository.save(draftInvoice(draftId, tenantId, null, oneLine())))
                .expectNextCount(1)
                .verifyComplete();

        Invoice issued = draftInvoice(issuedId, tenantId, null, oneLine());
        issued.issue();
        StepVerifier.create(invoiceRepository.save(issued))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invoiceQueryPort.countByTenantId(tenantId))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(invoiceQueryPort.findByTenantIdAndStatus(tenantId, InvoiceStatus.DRAFT))
                .assertNext(found -> assertThat(found.id()).isEqualTo(draftId))
                .verifyComplete();

        StepVerifier.create(invoiceQueryPort.findByTenantIdAndStatus(tenantId, InvoiceStatus.ISSUED))
                .assertNext(found -> assertThat(found.id()).isEqualTo(issuedId))
                .verifyComplete();
    }

    @Test
    void shouldEnforceSoftUniqueInvoiceNumberWithinSameTenant() {
        TenantId tenantId = TenantId.generate();
        InvoiceNumber number = InvoiceNumber.of("INV-DUP-0001");

        StepVerifier.create(invoiceRepository.save(draftInvoice(InvoiceId.generate(), tenantId, number, oneLine())))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invoiceRepository.save(draftInvoice(InvoiceId.generate(), tenantId, number, oneLine())))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    @Test
    void shouldAllowSameInvoiceNumberInDifferentTenants() {
        InvoiceNumber number = InvoiceNumber.of("SHARED-0001");

        StepVerifier.create(invoiceRepository.save(
                        draftInvoice(InvoiceId.generate(), TenantId.generate(), number, oneLine())))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invoiceRepository.save(
                        draftInvoice(InvoiceId.generate(), TenantId.generate(), number, oneLine())))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldAllowMultipleInvoicesWithoutNumber() {
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(invoiceRepository.save(draftInvoice(InvoiceId.generate(), tenantId, null, oneLine())))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(invoiceRepository.save(draftInvoice(InvoiceId.generate(), tenantId, null, oneLine())))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invoiceQueryPort.countByTenantId(tenantId))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void shouldReportInvoiceNumberExistenceHelpers() {
        TenantId tenantId = TenantId.generate();
        InvoiceId invoiceId = InvoiceId.generate();
        InvoiceNumber number = InvoiceNumber.of("EXIST-0001");

        StepVerifier.create(invoiceRepository.save(draftInvoice(invoiceId, tenantId, number, oneLine())))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(invoiceQueryPort.existsByTenantIdAndInvoiceNumber(tenantId, number))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(invoiceQueryPort.existsByTenantIdAndInvoiceNumberExcludingId(tenantId, number, invoiceId))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(invoiceQueryPort.existsByTenantIdAndInvoiceNumberExcludingId(
                        tenantId, number, InvoiceId.generate()))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldPersistOrganizationBillToAndVoidLifecycle() {
        InvoiceId invoiceId = InvoiceId.generate();
        TenantId tenantId = TenantId.generate();
        Invoice invoice = Invoice.create(
                invoiceId,
                tenantId,
                OrganizationId.of(UUID.randomUUID()),
                BillTo.organization(com.codecore.billing.domain.valueobject.BillToOrganizationId.of(UUID.randomUUID())),
                null,
                oneLine(),
                NOW
        );

        StepVerifier.create(invoiceRepository.save(invoice)
                        .flatMap(saved -> {
                            saved.voidInvoice();
                            return invoiceRepository.save(saved);
                        }))
                .assertNext(voided -> {
                    assertThat(voided.status()).isEqualTo(InvoiceStatus.VOIDED);
                    assertThat(voided.billTo().isOrganization()).isTrue();
                })
                .verifyComplete();
    }

    private static Invoice draftInvoice(
            InvoiceId invoiceId,
            TenantId tenantId,
            InvoiceNumber invoiceNumber,
            List<InvoiceLine> lines
    ) {
        return Invoice.create(
                invoiceId,
                tenantId,
                OrganizationId.of(UUID.randomUUID()),
                BillTo.patient(BillToPatientId.of(UUID.randomUUID())),
                invoiceNumber,
                lines,
                NOW
        );
    }

    private static List<InvoiceLine> oneLine() {
        return List.of(InvoiceLine.create(
                InvoiceLineId.generate(),
                LineDescription.of("Consultation"),
                Money.of("USD", 15000),
                null,
                null
        ));
    }

    private static List<InvoiceLine> twoLines() {
        return List.of(
                InvoiceLine.create(
                        InvoiceLineId.generate(),
                        LineDescription.of("Consultation"),
                        Money.of("USD", 15000),
                        null,
                        null
                ),
                InvoiceLine.create(
                        InvoiceLineId.generate(),
                        LineDescription.of("Material"),
                        Money.of("USD", 10000),
                        ItemId.of(UUID.randomUUID()),
                        null
                )
        );
    }
}
