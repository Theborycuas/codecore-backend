package com.codecore.billing.application.admin;

import com.codecore.billing.application.command.CreateInvoiceCommand;
import com.codecore.billing.application.command.InvoiceLineDraft;
import com.codecore.billing.application.command.UpdateInvoiceCommand;
import com.codecore.billing.application.port.out.InvoiceAdminQueryRepository;
import com.codecore.billing.application.port.out.InvoiceQueryPort;
import com.codecore.billing.application.port.out.InvoiceRepository;
import com.codecore.billing.application.port.out.TenantContextAccessor;
import com.codecore.billing.domain.exception.DuplicateInvoiceNumberException;
import com.codecore.billing.domain.exception.InvalidDomainValueException;
import com.codecore.billing.domain.exception.InvalidInvoiceStateException;
import com.codecore.billing.domain.exception.InvoicePatientMismatchException;
import com.codecore.billing.domain.exception.InvoiceReferenceNotFoundException;
import com.codecore.billing.domain.model.invoice.Invoice;
import com.codecore.billing.domain.model.invoice.InvoiceLine;
import com.codecore.billing.domain.valueobject.BillTo;
import com.codecore.billing.domain.valueobject.BillToPatientId;
import com.codecore.billing.domain.valueobject.InvoiceId;
import com.codecore.billing.domain.valueobject.LineDescription;
import com.codecore.billing.domain.valueobject.Money;
import com.codecore.billing.domain.valueobject.OrganizationId;
import com.codecore.billing.domain.valueobject.TenantId;
import com.codecore.encounter.contract.reference.EncounterReferencePort;
import com.codecore.encounter.contract.reference.EncounterReferenceView;
import com.codecore.encounter.domain.valueobject.EncounterStatus;
import com.codecore.inventory.contract.reference.ItemReferencePort;
import com.codecore.organization.contract.reference.OrganizationReferencePort;
import com.codecore.patient.contract.reference.PatientReferencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceAdministrationUseCaseTest {

    @Mock
    private TenantContextAccessor tenantContextAccessor;
    @Mock
    private InvoiceAdminQueryRepository invoiceAdminQueryRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceQueryPort invoiceQueryPort;
    @Mock
    private OrganizationReferencePort organizationReferencePort;
    @Mock
    private PatientReferencePort patientReferencePort;
    @Mock
    private ItemReferencePort itemReferencePort;
    @Mock
    private EncounterReferencePort encounterReferencePort;
    @Mock
    private TransactionalOperator transactionalOperator;

    private InvoiceAdministrationUseCaseImpl useCase;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        useCase = new InvoiceAdministrationUseCaseImpl(
                tenantContextAccessor,
                invoiceAdminQueryRepository,
                invoiceRepository,
                invoiceQueryPort,
                organizationReferencePort,
                patientReferencePort,
                itemReferencePort,
                encounterReferencePort,
                transactionalOperator
        );
        tenantId = TenantId.generate();
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(invoiceRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    @Test
    void shouldCreateInvoiceWithPatientBillTo() {
        UUID issuerId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));

        CreateInvoiceCommand command = new CreateInvoiceCommand(
                issuerId,
                patientId,
                null,
                null,
                "USD",
                List.of(new InvoiceLineDraft("Consultation", 15000, null, null))
        );

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> {
                    org.assertj.core.api.Assertions.assertThat(view.issuerOrganizationUuid()).isEqualTo(issuerId);
                    org.assertj.core.api.Assertions.assertThat(view.billToPatientUuid()).isEqualTo(patientId);
                    org.assertj.core.api.Assertions.assertThat(view.billToOrganizationUuid()).isNull();
                    org.assertj.core.api.Assertions.assertThat(view.totalAmountMinor()).isEqualTo(15000);
                    org.assertj.core.api.Assertions.assertThat(view.currency()).isEqualTo("USD");
                    org.assertj.core.api.Assertions.assertThat(view.status().name()).isEqualTo("DRAFT");
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectWhenIssuerOrganizationNotActive() {
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(false));

        CreateInvoiceCommand command = new CreateInvoiceCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                "USD",
                List.of(new InvoiceLineDraft("Line", 100, null, null))
        );

        StepVerifier.create(useCase.execute(command))
                .expectError(InvoiceReferenceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldRejectWhenBillToPatientNotActive() {
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(false));

        CreateInvoiceCommand command = new CreateInvoiceCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                "USD",
                List.of(new InvoiceLineDraft("Line", 100, null, null))
        );

        StepVerifier.create(useCase.execute(command))
                .expectError(InvoiceReferenceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldRejectBillToOrganizationEqualToIssuer() {
        UUID sameOrg = UUID.randomUUID();
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));

        CreateInvoiceCommand command = new CreateInvoiceCommand(
                sameOrg,
                null,
                sameOrg,
                null,
                "USD",
                List.of(new InvoiceLineDraft("Line", 100, null, null))
        );

        StepVerifier.create(useCase.execute(command))
                .expectError(InvalidDomainValueException.class)
                .verify();
    }

    @Test
    void shouldRejectBothOrNeitherBillTo() {
        CreateInvoiceCommand bothPresent = new CreateInvoiceCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "USD",
                List.of(new InvoiceLineDraft("Line", 100, null, null))
        );

        StepVerifier.create(useCase.execute(bothPresent))
                .expectError(InvalidDomainValueException.class)
                .verify();

        CreateInvoiceCommand neitherPresent = new CreateInvoiceCommand(
                UUID.randomUUID(),
                null,
                null,
                null,
                "USD",
                List.of(new InvoiceLineDraft("Line", 100, null, null))
        );

        StepVerifier.create(useCase.execute(neitherPresent))
                .expectError(InvalidDomainValueException.class)
                .verify();
    }

    @Test
    void shouldValidateLineItemReference() {
        UUID itemId = UUID.randomUUID();
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(itemReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(false));

        CreateInvoiceCommand command = new CreateInvoiceCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                "USD",
                List.of(new InvoiceLineDraft("Material", 100, itemId, null))
        );

        StepVerifier.create(useCase.execute(command))
                .expectError(InvoiceReferenceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldValidateLineEncounterReferenceAndPatientCoherence() {
        UUID billToPatientId = UUID.randomUUID();
        UUID encounterId = UUID.randomUUID();
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));

        com.codecore.encounter.domain.valueobject.EncounterId encounterRef =
                new com.codecore.encounter.domain.valueobject.EncounterId(encounterId);
        com.codecore.encounter.domain.valueobject.PatientId otherPatientRef =
                new com.codecore.encounter.domain.valueobject.PatientId(UUID.randomUUID());
        EncounterReferenceView mismatchedView =
                new EncounterReferenceView(encounterRef, otherPatientRef, EncounterStatus.IN_PROGRESS);
        when(encounterReferencePort.findLinkableByIdAndTenant(any(), any()))
                .thenReturn(Mono.just(Optional.of(mismatchedView)));

        CreateInvoiceCommand command = new CreateInvoiceCommand(
                UUID.randomUUID(),
                billToPatientId,
                null,
                null,
                "USD",
                List.of(new InvoiceLineDraft("Procedure", 100, null, encounterId))
        );

        StepVerifier.create(useCase.execute(command))
                .expectError(InvoicePatientMismatchException.class)
                .verify();
    }

    @Test
    void shouldRejectUnknownEncounterReference() {
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(encounterReferencePort.findLinkableByIdAndTenant(any(), any())).thenReturn(Mono.just(Optional.empty()));

        CreateInvoiceCommand command = new CreateInvoiceCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                "USD",
                List.of(new InvoiceLineDraft("Procedure", 100, null, UUID.randomUUID()))
        );

        StepVerifier.create(useCase.execute(command))
                .expectError(InvoiceReferenceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldRejectDuplicateInvoiceNumberOnCreate() {
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(invoiceQueryPort.existsByTenantIdAndInvoiceNumber(any(), any())).thenReturn(Mono.just(true));

        CreateInvoiceCommand command = new CreateInvoiceCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "INV-0001",
                "USD",
                List.of(new InvoiceLineDraft("Line", 100, null, null))
        );

        StepVerifier.create(useCase.execute(command))
                .expectError(DuplicateInvoiceNumberException.class)
                .verify();
    }

    @Test
    void shouldUpdateInvoiceInDraft() {
        Invoice existing = existingDraftInvoice();
        when(invoiceQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(existing));
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(invoiceQueryPort.existsByTenantIdAndInvoiceNumberExcludingId(any(), any(), any()))
                .thenReturn(Mono.just(false));

        UpdateInvoiceCommand command = new UpdateInvoiceCommand(
                existing.id(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "INV-0002",
                "USD",
                List.of(new InvoiceLineDraft("Updated line", 20000, null, null))
        );

        StepVerifier.create(useCase.execute(command))
                .assertNext(view -> org.assertj.core.api.Assertions.assertThat(view.totalAmountMinor()).isEqualTo(20000))
                .verifyComplete();
    }

    @Test
    void shouldIssueDraftInvoice() {
        Invoice existing = existingDraftInvoice();
        when(invoiceQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(existing));
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.issue(existing.id()))
                .assertNext(view -> org.assertj.core.api.Assertions.assertThat(view.status().name()).isEqualTo("ISSUED"))
                .verifyComplete();
    }

    @Test
    void shouldRejectIssueOfAlreadyIssuedInvoice() {
        Invoice existing = existingDraftInvoice();
        existing.issue();
        when(invoiceQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(existing));
        when(organizationReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(patientReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.issue(existing.id()))
                .expectError(InvalidInvoiceStateException.class)
                .verify();
    }

    @Test
    void shouldVoidDraftInvoiceWithoutReferenceRevalidation() {
        Invoice existing = existingDraftInvoice();
        when(invoiceQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(existing));

        StepVerifier.create(useCase.voidInvoice(existing.id()))
                .assertNext(view -> org.assertj.core.api.Assertions.assertThat(view.status().name()).isEqualTo("VOIDED"))
                .verifyComplete();
    }

    @Test
    void shouldRejectWhenNoLinesProvided() {
        CreateInvoiceCommand command = new CreateInvoiceCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                "USD",
                List.of()
        );

        StepVerifier.create(useCase.execute(command))
                .expectError(InvalidDomainValueException.class)
                .verify();
    }

    private Invoice existingDraftInvoice() {
        Instant now = Instant.now();
        return Invoice.reconstitute(
                InvoiceId.generate(),
                tenantId,
                OrganizationId.of(UUID.randomUUID()),
                BillTo.patient(BillToPatientId.of(UUID.randomUUID())),
                null,
                List.of(InvoiceLine.create(
                        com.codecore.billing.domain.valueobject.InvoiceLineId.generate(),
                        LineDescription.of("Original line"),
                        Money.of("USD", 15000),
                        null,
                        null
                )),
                com.codecore.billing.domain.valueobject.InvoiceStatus.DRAFT,
                now,
                now
        );
    }
}
