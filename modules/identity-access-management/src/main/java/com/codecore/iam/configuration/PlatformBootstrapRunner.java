package com.codecore.iam.configuration;

import com.codecore.iam.application.port.in.BootstrapPlatformUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Runs greenfield bootstrap on application startup when configured (PASO 15.9.2).
 */
@Component
public class PlatformBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformBootstrapRunner.class);

    private final BootstrapPlatformUseCase bootstrapPlatformUseCase;

    public PlatformBootstrapRunner(BootstrapPlatformUseCase bootstrapPlatformUseCase) {
        this.bootstrapPlatformUseCase = bootstrapPlatformUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapPlatformUseCase.executeIfNeeded()
                .onErrorResume(ex -> {
                    log.error("Platform bootstrap failed", ex);
                    return Mono.error(ex);
                })
                .block();
    }
}
