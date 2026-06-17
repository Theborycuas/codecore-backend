package com.codecore.iam.interfaces.http.admin;

import com.codecore.iam.application.port.in.GetAdminPermissionUseCase;
import com.codecore.iam.application.port.in.ListAdminPermissionsUseCase;
import com.codecore.iam.application.query.PageQueryParser;
import com.codecore.iam.domain.valueobject.PermissionId;
import com.codecore.iam.interfaces.http.admin.dto.PagedPermissionResponse;
import com.codecore.iam.interfaces.http.admin.dto.PermissionResponse;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping(IamAdminApiPaths.PERMISSIONS)
@Tag(name = "Permissions", description = "Global permission catalog (`permission:read`)")
public class IamPermissionAdminController {

    private final ListAdminPermissionsUseCase listAdminPermissionsUseCase;
    private final GetAdminPermissionUseCase getAdminPermissionUseCase;

    public IamPermissionAdminController(
            ListAdminPermissionsUseCase listAdminPermissionsUseCase,
            GetAdminPermissionUseCase getAdminPermissionUseCase
    ) {
        this.listAdminPermissionsUseCase = listAdminPermissionsUseCase;
        this.getAdminPermissionUseCase = getAdminPermissionUseCase;
    }

    @GetMapping
    @RequiresPermission("permission:read")
    public Mono<PagedPermissionResponse> listPermissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "code,asc") String sort
    ) {
        return listAdminPermissionsUseCase
                .execute(PageQueryParser.parsePermissionPageQuery(page, size, sort))
                .map(PagedPermissionResponse::from);
    }

    @GetMapping("/{id}")
    @RequiresPermission("permission:read")
    public Mono<PermissionResponse> getPermission(@PathVariable UUID id) {
        return getAdminPermissionUseCase.execute(new PermissionId(id)).map(PermissionResponse::from);
    }
}
