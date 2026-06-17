package com.codecore.iam.interfaces.http.admin;

import com.codecore.iam.application.authorization.IamPermissionCatalog;
import com.codecore.iam.interfaces.http.security.RequiresPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Foundation endpoint proving {@link RequiresPermission} on a productivo admin route (FASE 15.0).
 */
@RestController
@RequestMapping(IamAdminApiPaths.ADMINISTRATION)
public class IamAdministrationController {

    @GetMapping("/status")
    @RequiresPermission("role:read")
    public Mono<Map<String, String>> status() {
        return Mono.just(Map.of(
                "module", "identity-access-management",
                "administration", "operational",
                "permission", IamPermissionCatalog.ROLE_READ.value()
        ));
    }
}
