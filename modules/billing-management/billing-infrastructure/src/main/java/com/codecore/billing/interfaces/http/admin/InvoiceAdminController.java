package com.codecore.billing.interfaces.http.admin;

import com.codecore.billing.application.command.CreateInvoiceCommand;
import com.codecore.billing.application.command.InvoiceLineDraft;
import com.codecore.billing.application.command.UpdateInvoiceCommand;
import com.codecore.billing.application.port.in.CreateInvoiceUseCase;
import com.codecore.billing.application.port.in.GetInvoiceUseCase;
import com.codecore.billing.application.port.in.IssueInvoiceUseCase;
import com.codecore.billing.application.port.in.ListInvoicesUseCase;
import com.codecore.billing.application.port.in.UpdateInvoiceUseCase;
import com.codecore.billing.application.port.in.VoidInvoiceUseCase;
import com.codecore.billing.application.query.InvoiceListQuery;
import com.codecore.billing.application.query.PageQueryParser;
import com.codecore.billing.domain.valueobject.InvoiceId;
import com.codecore.billing.interfaces.http.admin.dto.CreateInvoiceRequest;
import com.codecore.billing.interfaces.http.admin.dto.InvoiceLineRequest;
import com.codecore.billing.interfaces.http.admin.dto.InvoiceResponse;
import com.codecore.billing.interfaces.http.admin.dto.PagedInvoiceResponse;
import com.codecore.billing.interfaces.http.admin.dto.UpdateInvoiceRequest;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(InvoiceAdminApiPaths.INVOICES)
@Tag(name = "Invoices", description = "Invoice commercial claim administration (`invoice:*` permissions)")
public class InvoiceAdminController {

    private final ListInvoicesUseCase listInvoicesUseCase;
    private final GetInvoiceUseCase getInvoiceUseCase;
    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final UpdateInvoiceUseCase updateInvoiceUseCase;
    private final IssueInvoiceUseCase issueInvoiceUseCase;
    private final VoidInvoiceUseCase voidInvoiceUseCase;

    public InvoiceAdminController(
            ListInvoicesUseCase listInvoicesUseCase,
            GetInvoiceUseCase getInvoiceUseCase,
            CreateInvoiceUseCase createInvoiceUseCase,
            UpdateInvoiceUseCase updateInvoiceUseCase,
            IssueInvoiceUseCase issueInvoiceUseCase,
            VoidInvoiceUseCase voidInvoiceUseCase
    ) {
        this.listInvoicesUseCase = listInvoicesUseCase;
        this.getInvoiceUseCase = getInvoiceUseCase;
        this.createInvoiceUseCase = createInvoiceUseCase;
        this.updateInvoiceUseCase = updateInvoiceUseCase;
        this.issueInvoiceUseCase = issueInvoiceUseCase;
        this.voidInvoiceUseCase = voidInvoiceUseCase;
    }

    @GetMapping
    @RequiresPermission("invoice:read")
    public Mono<PagedInvoiceResponse> listInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "DRAFT") String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID issuerOrganizationId,
            @RequestParam(required = false) UUID billToPatientId,
            @RequestParam(required = false) UUID billToOrganizationId
    ) {
        InvoiceListQuery filter = InvoiceListQuery.of(
                status,
                q,
                issuerOrganizationId,
                billToPatientId,
                billToOrganizationId
        );
        return listInvoicesUseCase
                .execute(filter, PageQueryParser.parseInvoicePageQuery(page, size, sort))
                .map(PagedInvoiceResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("invoice:read")
    public Mono<InvoiceResponse> getInvoice(@PathVariable UUID id) {
        return getInvoiceUseCase.execute(new InvoiceId(id)).map(InvoiceResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("invoice:create")
    public Mono<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        return createInvoiceUseCase.execute(toCreateCommand(request)).map(InvoiceResponse::from);
    }

    @PutMapping("/{id}")
    @RequiresPermission("invoice:update")
    public Mono<InvoiceResponse> updateInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest request
    ) {
        return updateInvoiceUseCase.execute(toUpdateCommand(id, request)).map(InvoiceResponse::from);
    }

    @PostMapping("/{id}/issue")
    @RequiresPermission("invoice:issue")
    public Mono<InvoiceResponse> issueInvoice(@PathVariable UUID id) {
        return issueInvoiceUseCase.issue(new InvoiceId(id)).map(InvoiceResponse::from);
    }

    @PostMapping("/{id}/void")
    @RequiresPermission("invoice:void")
    public Mono<InvoiceResponse> voidInvoice(@PathVariable UUID id) {
        return voidInvoiceUseCase.voidInvoice(new InvoiceId(id)).map(InvoiceResponse::from);
    }

    private static CreateInvoiceCommand toCreateCommand(CreateInvoiceRequest request) {
        return new CreateInvoiceCommand(
                request.issuerOrganizationId(),
                request.billToPatientId(),
                request.billToOrganizationId(),
                request.invoiceNumber(),
                request.currency(),
                toLineDrafts(request.lines())
        );
    }

    private static UpdateInvoiceCommand toUpdateCommand(UUID id, UpdateInvoiceRequest request) {
        return new UpdateInvoiceCommand(
                new InvoiceId(id),
                request.issuerOrganizationId(),
                request.billToPatientId(),
                request.billToOrganizationId(),
                request.invoiceNumber(),
                request.currency(),
                toLineDrafts(request.lines())
        );
    }

    private static List<InvoiceLineDraft> toLineDrafts(List<InvoiceLineRequest> lines) {
        return lines.stream()
                .map(line -> new InvoiceLineDraft(
                        line.description(),
                        line.amountMinor(),
                        line.itemId(),
                        line.encounterId()
                ))
                .toList();
    }
}
