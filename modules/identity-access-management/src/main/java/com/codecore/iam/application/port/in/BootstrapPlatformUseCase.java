package com.codecore.iam.application.port.in;

import com.codecore.iam.application.dto.BootstrapPlatformResult;
import reactor.core.publisher.Mono;

/**
 * Greenfield platform bootstrap — tenant + OWNER (PASO 15.9.2).
 */
public interface BootstrapPlatformUseCase {

    Mono<BootstrapPlatformResult> executeIfNeeded();
}
