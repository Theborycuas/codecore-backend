package com.codecore.organization.interfaces.http.admin;

import com.codecore.iam.interfaces.http.security.RequiresPermission;
import com.codecore.organization.application.command.CreateOrganizationCommand;
import com.codecore.organization.application.command.UpdateOrganizationCommand;
import com.codecore.organization.application.port.in.ActivateOrganizationUseCase;
import com.codecore.organization.application.port.in.ArchiveOrganizationUseCase;
import com.codecore.organization.application.port.in.CreateOrganizationUseCase;
import com.codecore.organization.application.port.in.GetOrganizationUseCase;
import com.codecore.organization.application.port.in.ListOrganizationsUseCase;
import com.codecore.organization.application.port.in.UpdateOrganizationUseCase;
import com.codecore.organization.application.query.PageQueryParser;
import com.codecore.organization.application.query.StructureListFilter;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.interfaces.http.admin.dto.CreateOrganizationRequest;
import com.codecore.organization.interfaces.http.admin.dto.OrganizationResponse;
import com.codecore.organization.interfaces.http.admin.dto.PagedOrganizationResponse;
import com.codecore.organization.interfaces.http.admin.dto.UpdateOrganizationRequest;
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

import java.util.UUID;

@RestController
@RequestMapping(OrgAdminApiPaths.ORGANIZATIONS)
@Tag(name = "Organizations", description = "Organization administration (`organization:*` permissions)")
public class OrganizationAdminController {

    private final ListOrganizationsUseCase listOrganizationsUseCase;
    private final GetOrganizationUseCase getOrganizationUseCase;
    private final CreateOrganizationUseCase createOrganizationUseCase;
    private final UpdateOrganizationUseCase updateOrganizationUseCase;
    private final ArchiveOrganizationUseCase archiveOrganizationUseCase;
    private final ActivateOrganizationUseCase activateOrganizationUseCase;

    public OrganizationAdminController(
            ListOrganizationsUseCase listOrganizationsUseCase,
            GetOrganizationUseCase getOrganizationUseCase,
            CreateOrganizationUseCase createOrganizationUseCase,
            UpdateOrganizationUseCase updateOrganizationUseCase,
            ArchiveOrganizationUseCase archiveOrganizationUseCase,
            ActivateOrganizationUseCase activateOrganizationUseCase
    ) {
        this.listOrganizationsUseCase = listOrganizationsUseCase;
        this.getOrganizationUseCase = getOrganizationUseCase;
        this.createOrganizationUseCase = createOrganizationUseCase;
        this.updateOrganizationUseCase = updateOrganizationUseCase;
        this.archiveOrganizationUseCase = archiveOrganizationUseCase;
        this.activateOrganizationUseCase = activateOrganizationUseCase;
    }

    @GetMapping
    @RequiresPermission("organization:read")
    public Mono<PagedOrganizationResponse> listOrganizations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status
    ) {
        StructureListFilter filter = StructureListFilter.parse(status);
        return listOrganizationsUseCase
                .execute(filter, PageQueryParser.parseOrganizationPageQuery(page, size, sort))
                .map(PagedOrganizationResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("organization:read")
    public Mono<OrganizationResponse> getOrganization(@PathVariable UUID id) {
        return getOrganizationUseCase.execute(new OrganizationId(id)).map(OrganizationResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("organization:create")
    public Mono<OrganizationResponse> createOrganization(@Valid @RequestBody CreateOrganizationRequest request) {
        CreateOrganizationCommand command = new CreateOrganizationCommand(request.code(), request.name());
        return createOrganizationUseCase.execute(command).map(OrganizationResponse::from);
    }

    @PutMapping("/{id}")
    @RequiresPermission("organization:update")
    public Mono<OrganizationResponse> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        UpdateOrganizationCommand command = new UpdateOrganizationCommand(
                new OrganizationId(id),
                request.name()
        );
        return updateOrganizationUseCase.execute(command).map(OrganizationResponse::from);
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission("organization:archive")
    public Mono<OrganizationResponse> archiveOrganization(@PathVariable UUID id) {
        return archiveOrganizationUseCase.archive(new OrganizationId(id)).map(OrganizationResponse::from);
    }

    @PostMapping("/{id}/activate")
    @RequiresPermission("organization:update")
    public Mono<OrganizationResponse> activateOrganization(@PathVariable UUID id) {
        return activateOrganizationUseCase.activate(new OrganizationId(id)).map(OrganizationResponse::from);
    }
}
