package com.codecore.iam.interfaces.http.admin;

import com.codecore.iam.application.command.CreateAdminRoleCommand;
import com.codecore.iam.application.command.UpdateAdminRoleCommand;
import com.codecore.iam.application.port.in.CreateAdminRoleUseCase;
import com.codecore.iam.application.port.in.DeleteAdminRoleUseCase;
import com.codecore.iam.application.port.in.GetAdminRoleUseCase;
import com.codecore.iam.application.port.in.ListAdminRolesUseCase;
import com.codecore.iam.application.port.in.UpdateAdminRoleUseCase;
import com.codecore.iam.application.query.PageQueryParser;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.interfaces.http.admin.dto.CreateRoleRequest;
import com.codecore.iam.interfaces.http.admin.dto.PagedRoleResponse;
import com.codecore.iam.interfaces.http.admin.dto.RoleResponse;
import com.codecore.iam.interfaces.http.admin.dto.UpdateRoleRequest;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping(IamAdminApiPaths.ROLES)
@Tag(name = "Roles", description = "Tenant role administration (`role:*` permissions)")
public class IamRoleAdminController {

    private final ListAdminRolesUseCase listAdminRolesUseCase;
    private final GetAdminRoleUseCase getAdminRoleUseCase;
    private final CreateAdminRoleUseCase createAdminRoleUseCase;
    private final UpdateAdminRoleUseCase updateAdminRoleUseCase;
    private final DeleteAdminRoleUseCase deleteAdminRoleUseCase;

    public IamRoleAdminController(
            ListAdminRolesUseCase listAdminRolesUseCase,
            GetAdminRoleUseCase getAdminRoleUseCase,
            CreateAdminRoleUseCase createAdminRoleUseCase,
            UpdateAdminRoleUseCase updateAdminRoleUseCase,
            DeleteAdminRoleUseCase deleteAdminRoleUseCase
    ) {
        this.listAdminRolesUseCase = listAdminRolesUseCase;
        this.getAdminRoleUseCase = getAdminRoleUseCase;
        this.createAdminRoleUseCase = createAdminRoleUseCase;
        this.updateAdminRoleUseCase = updateAdminRoleUseCase;
        this.deleteAdminRoleUseCase = deleteAdminRoleUseCase;
    }

    @GetMapping
    @RequiresPermission("role:read")
    public Mono<PagedRoleResponse> listRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        return listAdminRolesUseCase
                .execute(PageQueryParser.parseRolePageQuery(page, size, sort))
                .map(PagedRoleResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("role:read")
    public Mono<RoleResponse> getRole(@PathVariable UUID id) {
        return getAdminRoleUseCase.execute(new RoleId(id)).map(RoleResponse::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequiresPermission("role:create")
    public Mono<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        CreateAdminRoleCommand command = new CreateAdminRoleCommand(request.code(), request.name());
        return createAdminRoleUseCase.execute(command).map(RoleResponse::from);
    }

    @PutMapping("/{id}")
    @RequiresPermission("role:update")
    public Mono<RoleResponse> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        UpdateAdminRoleCommand command = new UpdateAdminRoleCommand(
                new RoleId(id),
                request.name(),
                request.status()
        );
        return updateAdminRoleUseCase.execute(command).map(RoleResponse::from);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresPermission("role:delete")
    public Mono<Void> deleteRole(@PathVariable UUID id) {
        return deleteAdminRoleUseCase.delete(new RoleId(id));
    }
}
