package com.codecore.organization.interfaces.http.admin;

import com.codecore.iam.interfaces.http.security.RequiresPermission;
import com.codecore.organization.application.command.CreateOfficeCommand;
import com.codecore.organization.application.command.UpdateOfficeCommand;
import com.codecore.organization.application.port.in.ActivateOfficeUseCase;
import com.codecore.organization.application.port.in.ArchiveOfficeUseCase;
import com.codecore.organization.application.port.in.CreateOfficeUseCase;
import com.codecore.organization.application.port.in.GetOfficeUseCase;
import com.codecore.organization.application.port.in.ListOfficesUseCase;
import com.codecore.organization.application.port.in.UpdateOfficeUseCase;
import com.codecore.organization.application.query.PageQueryParser;
import com.codecore.organization.application.query.StructureListFilter;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.interfaces.http.admin.dto.CreateOfficeRequest;
import com.codecore.organization.interfaces.http.admin.dto.OfficeResponse;
import com.codecore.organization.interfaces.http.admin.dto.PagedOfficeResponse;
import com.codecore.organization.interfaces.http.admin.dto.UpdateOfficeRequest;
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
@RequestMapping(OrgAdminApiPaths.OFFICES)
@Tag(name = "Offices", description = "Office administration (`office:*` permissions)")
public class OfficeAdminController {

    private final ListOfficesUseCase listOfficesUseCase;
    private final GetOfficeUseCase getOfficeUseCase;
    private final CreateOfficeUseCase createOfficeUseCase;
    private final UpdateOfficeUseCase updateOfficeUseCase;
    private final ArchiveOfficeUseCase archiveOfficeUseCase;
    private final ActivateOfficeUseCase activateOfficeUseCase;

    public OfficeAdminController(
            ListOfficesUseCase listOfficesUseCase,
            GetOfficeUseCase getOfficeUseCase,
            CreateOfficeUseCase createOfficeUseCase,
            UpdateOfficeUseCase updateOfficeUseCase,
            ArchiveOfficeUseCase archiveOfficeUseCase,
            ActivateOfficeUseCase activateOfficeUseCase
    ) {
        this.listOfficesUseCase = listOfficesUseCase;
        this.getOfficeUseCase = getOfficeUseCase;
        this.createOfficeUseCase = createOfficeUseCase;
        this.updateOfficeUseCase = updateOfficeUseCase;
        this.archiveOfficeUseCase = archiveOfficeUseCase;
        this.activateOfficeUseCase = activateOfficeUseCase;
    }

    @GetMapping
    @RequiresPermission("office:read")
    public Mono<PagedOfficeResponse> listOffices(
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(defaultValue = "ACTIVE") String status
    ) {
        StructureListFilter filter = StructureListFilter.parse(status);
        return listOfficesUseCase
                .execute(
                        new OrganizationId(organizationId),
                        filter,
                        PageQueryParser.parseOfficePageQuery(page, size, sort)
                )
                .map(PagedOfficeResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("office:read")
    public Mono<OfficeResponse> getOffice(@PathVariable UUID id) {
        return getOfficeUseCase.execute(new OfficeId(id)).map(OfficeResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("office:create")
    public Mono<OfficeResponse> createOffice(@Valid @RequestBody CreateOfficeRequest request) {
        CreateOfficeCommand command = new CreateOfficeCommand(
                request.organizationId(),
                request.code(),
                request.name()
        );
        return createOfficeUseCase.execute(command).map(OfficeResponse::from);
    }

    @PutMapping("/{id}")
    @RequiresPermission("office:update")
    public Mono<OfficeResponse> updateOffice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOfficeRequest request
    ) {
        UpdateOfficeCommand command = new UpdateOfficeCommand(new OfficeId(id), request.name());
        return updateOfficeUseCase.execute(command).map(OfficeResponse::from);
    }

    @PostMapping("/{id}/archive")
    @RequiresPermission("office:archive")
    public Mono<OfficeResponse> archiveOffice(@PathVariable UUID id) {
        return archiveOfficeUseCase.archive(new OfficeId(id)).map(OfficeResponse::from);
    }

    @PostMapping("/{id}/activate")
    @RequiresPermission("office:update")
    public Mono<OfficeResponse> activateOffice(@PathVariable UUID id) {
        return activateOfficeUseCase.activate(new OfficeId(id)).map(OfficeResponse::from);
    }
}
