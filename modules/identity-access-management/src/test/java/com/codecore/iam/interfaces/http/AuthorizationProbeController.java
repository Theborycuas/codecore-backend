package com.codecore.iam.interfaces.http;

import com.codecore.iam.interfaces.http.security.RequiresPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Test-only endpoint for {@link RequiresPermission} HTTP integration tests.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthorizationProbeController {

    public static final String PROBE_PERMISSION = "probe:access";

    @GetMapping("/authorization-probe")
    @RequiresPermission(PROBE_PERMISSION)
    public Mono<Map<String, String>> authorizationProbe() {
        return Mono.just(Map.of("authorized", "true"));
    }

    @GetMapping("/user-create-probe")
    @RequiresPermission("user:create")
    public Mono<Map<String, String>> userCreateProbe() {
        return Mono.just(Map.of("authorized", "true"));
    }
}
