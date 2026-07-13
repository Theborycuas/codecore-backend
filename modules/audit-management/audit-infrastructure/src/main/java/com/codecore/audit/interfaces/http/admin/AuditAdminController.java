package com.codecore.audit.interfaces.http.admin;

import com.codecore.audit.application.port.in.GetAuditEntryUseCase;
import com.codecore.audit.application.port.in.ListAuditEntriesUseCase;
import com.codecore.audit.application.query.AuditListQuery;
import com.codecore.audit.application.query.PageQueryParser;
import com.codecore.audit.domain.valueobject.AuditEntryId;
import com.codecore.audit.interfaces.http.admin.dto.AuditEntryResponse;
import com.codecore.audit.interfaces.http.admin.dto.PagedAuditEntryResponse;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Audit administration HTTP API — read-only (ADR-020). Append is via {@code AuditAppendPort}.
 */
@RestController
@RequestMapping(AuditAdminApiPaths.ENTRIES)
@Tag(name = "Audit", description = "AuditEntry administration (`audit:read` permission)")
public class AuditAdminController {

    private final ListAuditEntriesUseCase listAuditEntriesUseCase;
    private final GetAuditEntryUseCase getAuditEntryUseCase;

    public AuditAdminController(
            ListAuditEntriesUseCase listAuditEntriesUseCase,
            GetAuditEntryUseCase getAuditEntryUseCase
    ) {
        this.listAuditEntriesUseCase = listAuditEntriesUseCase;
        this.getAuditEntryUseCase = getAuditEntryUseCase;
    }

    @GetMapping
    @RequiresPermission("audit:read")
    public Mono<PagedAuditEntryResponse> listEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt,desc") String sort,
            @RequestParam(required = false) String actionCode,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) UUID resourceId
    ) {
        AuditListQuery filter = AuditListQuery.of(actionCode, resourceType, resourceId);
        return listAuditEntriesUseCase
                .execute(filter, PageQueryParser.parseAuditPageQuery(page, size, sort))
                .map(PagedAuditEntryResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("audit:read")
    public Mono<AuditEntryResponse> getEntry(@PathVariable UUID id) {
        return getAuditEntryUseCase.execute(new AuditEntryId(id)).map(AuditEntryResponse::from);
    }
}
