package com.codecore.iam.interfaces.http.admin;

import com.codecore.iam.application.command.UpdateAdminTenantCommand;
import com.codecore.iam.application.port.in.GetAdminTenantUseCase;
import com.codecore.iam.application.port.in.UpdateAdminTenantUseCase;
import com.codecore.iam.interfaces.http.admin.dto.TenantResponse;
import com.codecore.iam.interfaces.http.admin.dto.UpdateTenantRequest;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(IamAdminApiPaths.TENANTS)
public class IamTenantAdminController {

    private final GetAdminTenantUseCase getAdminTenantUseCase;
    private final UpdateAdminTenantUseCase updateAdminTenantUseCase;

    public IamTenantAdminController(
            GetAdminTenantUseCase getAdminTenantUseCase,
            UpdateAdminTenantUseCase updateAdminTenantUseCase
    ) {
        this.getAdminTenantUseCase = getAdminTenantUseCase;
        this.updateAdminTenantUseCase = updateAdminTenantUseCase;
    }

    @GetMapping("/current")
    @RequiresPermission("tenant:read")
    public Mono<TenantResponse> getCurrentTenant() {
        return getAdminTenantUseCase.execute().map(TenantResponse::from);
    }

    @PutMapping("/current")
    @RequiresPermission("tenant:update")
    public Mono<TenantResponse> updateCurrentTenant(@Valid @RequestBody UpdateTenantRequest request) {
        UpdateAdminTenantCommand command = new UpdateAdminTenantCommand(request.name(), request.status());
        return updateAdminTenantUseCase.execute(command).map(TenantResponse::from);
    }
}
