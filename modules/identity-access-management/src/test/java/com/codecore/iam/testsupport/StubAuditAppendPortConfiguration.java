package com.codecore.iam.testsupport;

import com.codecore.audit.contract.append.AuditAppendPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Stub {@link AuditAppendPort} for IAM / Access integration tests that import
 * {@code IamModuleConfiguration} without the full Audit infrastructure stack.
 */
@TestConfiguration
public class StubAuditAppendPortConfiguration {

    @Bean
    public AuditAppendPort stubAuditAppendPort() {
        return cmd -> Mono.just(UUID.randomUUID());
    }
}
