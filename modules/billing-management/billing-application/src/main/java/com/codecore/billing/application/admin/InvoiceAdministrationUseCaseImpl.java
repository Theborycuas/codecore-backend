package com.codecore.billing.application.admin;

import com.codecore.billing.application.command.CreateInvoiceCommand;
import com.codecore.billing.application.command.InvoiceLineDraft;
import com.codecore.billing.application.command.UpdateInvoiceCommand;
import com.codecore.billing.application.dto.AdminInvoiceLineView;
import com.codecore.billing.application.dto.AdminInvoiceView;
import com.codecore.billing.application.dto.PagedResult;
import com.codecore.billing.application.port.in.CreateInvoiceUseCase;
import com.codecore.billing.application.port.in.GetInvoiceUseCase;
import com.codecore.billing.application.port.in.IssueInvoiceUseCase;
import com.codecore.billing.application.port.in.ListInvoicesUseCase;
import com.codecore.billing.application.port.in.UpdateInvoiceUseCase;
import com.codecore.billing.application.port.in.VoidInvoiceUseCase;
import com.codecore.billing.application.port.out.InvoiceAdminQueryRepository;
import com.codecore.billing.application.port.out.InvoiceQueryPort;
import com.codecore.billing.application.port.out.InvoiceRepository;
import com.codecore.billing.application.port.out.TenantContextAccessor;
import com.codecore.billing.application.query.InvoiceListQuery;
import com.codecore.billing.application.query.PageQuery;
import com.codecore.billing.domain.exception.DuplicateInvoiceNumberException;
import com.codecore.billing.domain.exception.InvalidDomainValueException;
import com.codecore.billing.domain.exception.InvoiceNotFoundException;
import com.codecore.billing.domain.exception.InvoicePatientMismatchException;
import com.codecore.billing.domain.exception.InvoiceReferenceNotFoundException;
import com.codecore.billing.domain.model.invoice.Invoice;
import com.codecore.billing.domain.model.invoice.InvoiceLine;
import com.codecore.billing.domain.valueobject.BillTo;
import com.codecore.billing.domain.valueobject.BillToOrganizationId;
import com.codecore.billing.domain.valueobject.BillToPatientId;
import com.codecore.billing.domain.valueobject.EncounterId;
import com.codecore.billing.domain.valueobject.InvoiceId;
import com.codecore.billing.domain.valueobject.InvoiceLineId;
import com.codecore.billing.domain.valueobject.InvoiceNumber;
import com.codecore.billing.domain.valueobject.ItemId;
import com.codecore.billing.domain.valueobject.LineDescription;
import com.codecore.billing.domain.valueobject.Money;
import com.codecore.billing.domain.valueobject.OrganizationId;
import com.codecore.billing.domain.valueobject.TenantId;
import com.codecore.encounter.contract.reference.EncounterReferencePort;
import com.codecore.inventory.contract.reference.ItemReferencePort;
import com.codecore.organization.contract.reference.OrganizationReferencePort;
import com.codecore.patient.contract.reference.PatientReferencePort;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Invoice administration use cases (PASO 21.6) — multi-ReferencePort write validation
 * (ADR-013 / ADR-017): Organization (issuer + bill-to), Patient (bill-to), Item (line),
 * Encounter (line, with bill-to Patient coherence).
 */
public final class InvoiceAdministrationUseCaseImpl
        implements ListInvoicesUseCase,
        GetInvoiceUseCase,
        CreateInvoiceUseCase,
        UpdateInvoiceUseCase,
        IssueInvoiceUseCase,
        VoidInvoiceUseCase {

    private final TenantContextAccessor tenantContextAccessor;
    private final InvoiceAdminQueryRepository invoiceAdminQueryRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceQueryPort invoiceQueryPort;
    private final OrganizationReferencePort organizationReferencePort;
    private final PatientReferencePort patientReferencePort;
    private final ItemReferencePort itemReferencePort;
    private final EncounterReferencePort encounterReferencePort;
    private final TransactionalOperator transactionalOperator;

    public InvoiceAdministrationUseCaseImpl(
            TenantContextAccessor tenantContextAccessor,
            InvoiceAdminQueryRepository invoiceAdminQueryRepository,
            InvoiceRepository invoiceRepository,
            InvoiceQueryPort invoiceQueryPort,
            OrganizationReferencePort organizationReferencePort,
            PatientReferencePort patientReferencePort,
            ItemReferencePort itemReferencePort,
            EncounterReferencePort encounterReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        this.tenantContextAccessor = Objects.requireNonNull(tenantContextAccessor, "tenantContextAccessor");
        this.invoiceAdminQueryRepository = Objects.requireNonNull(
                invoiceAdminQueryRepository,
                "invoiceAdminQueryRepository"
        );
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository");
        this.invoiceQueryPort = Objects.requireNonNull(invoiceQueryPort, "invoiceQueryPort");
        this.organizationReferencePort = Objects.requireNonNull(
                organizationReferencePort,
                "organizationReferencePort"
        );
        this.patientReferencePort = Objects.requireNonNull(patientReferencePort, "patientReferencePort");
        this.itemReferencePort = Objects.requireNonNull(itemReferencePort, "itemReferencePort");
        this.encounterReferencePort = Objects.requireNonNull(encounterReferencePort, "encounterReferencePort");
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<PagedResult<AdminInvoiceView>> execute(InvoiceListQuery filter, PageQuery pageQuery) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> invoiceAdminQueryRepository.countByTenantId(tenantId, filter)
                        .flatMap(total -> invoiceAdminQueryRepository
                                .findByTenantId(tenantId, filter, pageQuery)
                                .collectList()
                                .map(content -> PagedResult.of(
                                        content,
                                        pageQuery.page(),
                                        pageQuery.size(),
                                        total
                                ))));
    }

    @Override
    public Mono<AdminInvoiceView> execute(InvoiceId invoiceId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, invoiceId).map(this::toView));
    }

    @Override
    public Mono<AdminInvoiceView> execute(CreateInvoiceCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> {
                    OrganizationId issuer = requireOrganizationId(command.issuerOrganizationId());
                    BillTo billTo = toBillTo(command.billToPatientId(), command.billToOrganizationId());
                    List<InvoiceLine> lines = toLines(command.currency(), command.lines());
                    InvoiceNumber invoiceNumber = toOptionalInvoiceNumber(command.invoiceNumber());

                    return validateWriteRefs(tenantId, issuer, billTo, lines)
                            .then(Mono.defer(() -> validateInvoiceNumberUnique(tenantId, invoiceNumber, null)))
                            .then(Mono.defer(() -> {
                                Invoice invoice = Invoice.create(
                                        InvoiceId.generate(),
                                        tenantId,
                                        issuer,
                                        billTo,
                                        invoiceNumber,
                                        lines,
                                        Instant.now()
                                );
                                return invoiceRepository.save(invoice).map(this::toView);
                            }));
                })
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminInvoiceView> execute(UpdateInvoiceCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, command.invoiceId())
                        .flatMap(invoice -> {
                            OrganizationId issuer = requireOrganizationId(command.issuerOrganizationId());
                            BillTo billTo = toBillTo(command.billToPatientId(), command.billToOrganizationId());
                            List<InvoiceLine> lines = toLines(command.currency(), command.lines());
                            InvoiceNumber invoiceNumber = toOptionalInvoiceNumber(command.invoiceNumber());

                            return validateWriteRefs(tenantId, issuer, billTo, lines)
                                    .then(Mono.defer(() -> validateInvoiceNumberUnique(
                                            tenantId,
                                            invoiceNumber,
                                            invoice.id()
                                    )))
                                    .then(Mono.defer(() -> {
                                        invoice.updateContent(issuer, billTo, invoiceNumber, lines);
                                        return invoiceRepository.save(invoice);
                                    }));
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminInvoiceView> issue(InvoiceId invoiceId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, invoiceId)
                        .flatMap(invoice -> validateWriteRefs(
                                tenantId,
                                invoice.issuerOrganizationId(),
                                invoice.billTo(),
                                invoice.lines()
                        ).then(Mono.defer(() -> {
                            invoice.issue();
                            return invoiceRepository.save(invoice);
                        })))
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminInvoiceView> voidInvoice(InvoiceId invoiceId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, invoiceId)
                        .flatMap(invoice -> {
                            invoice.voidInvoice();
                            return invoiceRepository.save(invoice);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    private Mono<Invoice> loadInTenant(TenantId tenantId, InvoiceId invoiceId) {
        return invoiceQueryPort.findByIdAndTenantId(invoiceId, tenantId)
                .switchIfEmpty(Mono.error(new InvoiceNotFoundException(
                        "Invoice not found in tenant context")));
    }

    /**
     * Write-time ReferencePort validation (ADR-017 §11). {@code voidInvoice} skips this.
     */
    private Mono<Void> validateWriteRefs(TenantId tenantId, OrganizationId issuer, BillTo billTo, List<InvoiceLine> lines) {
        com.codecore.organization.domain.valueobject.TenantId orgTenant =
                new com.codecore.organization.domain.valueobject.TenantId(tenantId.value());
        com.codecore.organization.domain.valueobject.OrganizationId issuerRef =
                new com.codecore.organization.domain.valueobject.OrganizationId(issuer.value());

        return organizationReferencePort.existsActiveByIdAndTenant(issuerRef, orgTenant)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.<Void>error(new InvoiceReferenceNotFoundException(
                                "Issuer organization not found or not ACTIVE in tenant"));
                    }
                    return validateBillTo(tenantId, orgTenant, billTo);
                })
                .then(Mono.defer(() -> validateLines(tenantId, billTo, lines)));
    }

    private Mono<Void> validateBillTo(
            TenantId tenantId,
            com.codecore.organization.domain.valueobject.TenantId orgTenant,
            BillTo billTo
    ) {
        if (billTo.isPatient()) {
            com.codecore.patient.domain.valueobject.PatientId patientRef =
                    new com.codecore.patient.domain.valueobject.PatientId(
                            billTo.patientId().orElseThrow().value());
            com.codecore.patient.domain.valueobject.TenantId patientTenant =
                    new com.codecore.patient.domain.valueobject.TenantId(tenantId.value());
            return patientReferencePort.existsActiveByIdAndTenant(patientRef, patientTenant)
                    .flatMap(exists -> exists
                            ? Mono.<Void>empty()
                            : Mono.error(new InvoiceReferenceNotFoundException(
                                    "Bill-to patient not found or not ACTIVE in tenant")));
        }
        com.codecore.organization.domain.valueobject.OrganizationId billToOrgRef =
                new com.codecore.organization.domain.valueobject.OrganizationId(
                        billTo.organizationId().orElseThrow().value());
        return organizationReferencePort.existsActiveByIdAndTenant(billToOrgRef, orgTenant)
                .flatMap(exists -> exists
                        ? Mono.<Void>empty()
                        : Mono.error(new InvoiceReferenceNotFoundException(
                                "Bill-to organization not found or not ACTIVE in tenant")));
    }

    /**
     * Soft-uniqueness of {@code (tenantId, invoiceNumber)} (ADR-017 §5) — skipped when the
     * command carries no invoice number.
     */
    private Mono<Void> validateInvoiceNumberUnique(TenantId tenantId, InvoiceNumber invoiceNumber, InvoiceId excludeInvoiceId) {
        if (invoiceNumber == null) {
            return Mono.empty();
        }
        Mono<Boolean> exists = excludeInvoiceId == null
                ? invoiceQueryPort.existsByTenantIdAndInvoiceNumber(tenantId, invoiceNumber)
                : invoiceQueryPort.existsByTenantIdAndInvoiceNumberExcludingId(tenantId, invoiceNumber, excludeInvoiceId);
        return exists.flatMap(duplicate -> duplicate
                ? Mono.<Void>error(new DuplicateInvoiceNumberException(
                        "Invoice number already used by another invoice in this tenant"))
                : Mono.empty());
    }

    private Mono<Void> validateLines(TenantId tenantId, BillTo billTo, List<InvoiceLine> lines) {
        return Flux.fromIterable(lines)
                .concatMap(line -> validateLine(tenantId, billTo, line))
                .then();
    }

    private Mono<Void> validateLine(TenantId tenantId, BillTo billTo, InvoiceLine line) {
        Mono<Void> itemCheck = line.itemId()
                .map(itemId -> validateItem(tenantId, itemId))
                .orElseGet(Mono::empty);
        Mono<Void> encounterCheck = line.encounterId()
                .map(encounterId -> validateEncounter(tenantId, billTo, encounterId))
                .orElseGet(Mono::empty);
        return itemCheck.then(encounterCheck);
    }

    private Mono<Void> validateItem(TenantId tenantId, ItemId itemId) {
        com.codecore.inventory.domain.valueobject.ItemId itemRef =
                new com.codecore.inventory.domain.valueobject.ItemId(itemId.value());
        com.codecore.inventory.domain.valueobject.TenantId itemTenant =
                new com.codecore.inventory.domain.valueobject.TenantId(tenantId.value());
        return itemReferencePort.existsActiveByIdAndTenant(itemRef, itemTenant)
                .flatMap(exists -> exists
                        ? Mono.<Void>empty()
                        : Mono.error(new InvoiceReferenceNotFoundException(
                                "Line item not found or not ACTIVE in tenant")));
    }

    private Mono<Void> validateEncounter(TenantId tenantId, BillTo billTo, EncounterId encounterId) {
        com.codecore.encounter.domain.valueobject.EncounterId encounterRef =
                new com.codecore.encounter.domain.valueobject.EncounterId(encounterId.value());
        com.codecore.encounter.domain.valueobject.TenantId encounterTenant =
                new com.codecore.encounter.domain.valueobject.TenantId(tenantId.value());

        return encounterReferencePort.findLinkableByIdAndTenant(encounterRef, encounterTenant)
                .flatMap(optionalView -> {
                    if (optionalView.isEmpty()) {
                        return Mono.<Void>error(new InvoiceReferenceNotFoundException(
                                "Line encounter not found, not linkable, or not in tenant"));
                    }
                    if (billTo.isPatient()) {
                        UUID billToPatientId = billTo.patientId().orElseThrow().value();
                        if (!optionalView.get().patientId().value().equals(billToPatientId)) {
                            return Mono.<Void>error(new InvoicePatientMismatchException(
                                    "Line encounter patientId must match Invoice bill-to patient"));
                        }
                    }
                    return Mono.<Void>empty();
                });
    }

    private static OrganizationId requireOrganizationId(UUID issuerOrganizationId) {
        if (issuerOrganizationId == null) {
            throw new InvalidDomainValueException("issuerOrganizationId is required");
        }
        return OrganizationId.of(issuerOrganizationId);
    }

    private static BillTo toBillTo(UUID billToPatientId, UUID billToOrganizationId) {
        boolean hasPatient = billToPatientId != null;
        boolean hasOrganization = billToOrganizationId != null;
        if (hasPatient == hasOrganization) {
            throw new InvalidDomainValueException(
                    "Exactly one of billToPatientId or billToOrganizationId is required");
        }
        return hasPatient
                ? BillTo.patient(BillToPatientId.of(billToPatientId))
                : BillTo.organization(BillToOrganizationId.of(billToOrganizationId));
    }

    private static InvoiceNumber toOptionalInvoiceNumber(String invoiceNumber) {
        return invoiceNumber == null || invoiceNumber.isBlank() ? null : InvoiceNumber.of(invoiceNumber);
    }

    private static List<InvoiceLine> toLines(String currency, List<InvoiceLineDraft> drafts) {
        if (currency == null || currency.isBlank()) {
            throw new InvalidDomainValueException("currency is required");
        }
        if (drafts == null || drafts.isEmpty()) {
            throw new InvalidDomainValueException("Invoice must have at least one line");
        }
        return drafts.stream()
                .map(draft -> InvoiceLine.create(
                        InvoiceLineId.generate(),
                        LineDescription.of(draft.description()),
                        Money.of(currency, draft.amountMinor()),
                        draft.itemId() == null ? null : ItemId.of(draft.itemId()),
                        draft.encounterId() == null ? null : EncounterId.of(draft.encounterId())
                ))
                .toList();
    }

    private AdminInvoiceView toView(Invoice invoice) {
        List<AdminInvoiceLineView> lineViews = invoice.lines().stream()
                .map(line -> new AdminInvoiceLineView(
                        line.id(),
                        line.description().value(),
                        line.amount().amountMinor(),
                        line.amount().currency(),
                        line.itemId().orElse(null),
                        line.encounterId().orElse(null)
                ))
                .toList();
        return new AdminInvoiceView(
                invoice.id(),
                invoice.tenantId(),
                invoice.issuerOrganizationId(),
                invoice.billTo().patientId().orElse(null),
                invoice.billTo().organizationId().orElse(null),
                invoice.invoiceNumber().map(InvoiceNumber::value).orElse(null),
                invoice.currency(),
                lineViews,
                invoice.totalAmountMinor(),
                invoice.status(),
                invoice.createdAt(),
                invoice.updatedAt()
        );
    }
}
