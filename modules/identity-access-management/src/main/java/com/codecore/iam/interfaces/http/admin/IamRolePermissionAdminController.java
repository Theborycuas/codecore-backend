package com.codecore.iam.interfaces.http.admin;

import com.codecore.iam.application.command.ReplaceAdminRolePermissionsCommand;
import com.codecore.iam.application.port.in.GetAdminRolePermissionsUseCase;
import com.codecore.iam.application.port.in.ReplaceAdminRolePermissionsUseCase;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.interfaces.http.admin.dto.ReplaceRolePermissionsRequest;
import com.codecore.iam.interfaces.http.admin.dto.RolePermissionResponse;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(IamAdminApiPaths.ROLES + "/{roleId}/permissions")
@Tag(name = "Role Permissions", description = "Role ↔ permission assignments (`permission:assign`)")
public class IamRolePermissionAdminController {

    private final GetAdminRolePermissionsUseCase getAdminRolePermissionsUseCase;
    private final ReplaceAdminRolePermissionsUseCase replaceAdminRolePermissionsUseCase;

    public IamRolePermissionAdminController(
            GetAdminRolePermissionsUseCase getAdminRolePermissionsUseCase,
            ReplaceAdminRolePermissionsUseCase replaceAdminRolePermissionsUseCase
    ) {
        this.getAdminRolePermissionsUseCase = getAdminRolePermissionsUseCase;
        this.replaceAdminRolePermissionsUseCase = replaceAdminRolePermissionsUseCase;
    }

    @GetMapping
    @RequiresPermission("permission:assign")
    public Mono<List<RolePermissionResponse>> listRolePermissions(@PathVariable UUID roleId) {
        return getAdminRolePermissionsUseCase
                .execute(new RoleId(roleId))
                .map(views -> views.stream().map(RolePermissionResponse::from).toList());
    }

    @PutMapping
    @RequiresPermission("permission:assign")
    public Mono<List<RolePermissionResponse>> replaceRolePermissions(
            @PathVariable UUID roleId,
            @Valid @RequestBody ReplaceRolePermissionsRequest request
    ) {
        ReplaceAdminRolePermissionsCommand command = new ReplaceAdminRolePermissionsCommand(
                new RoleId(roleId),
                request.permissionIds()
        );
        return replaceAdminRolePermissionsUseCase
                .execute(command)
                .map(views -> views.stream().map(RolePermissionResponse::from).toList());
    }
}
