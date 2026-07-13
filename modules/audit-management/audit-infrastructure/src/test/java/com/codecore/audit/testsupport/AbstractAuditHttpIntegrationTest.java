package com.codecore.audit.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Shared HTTP + Testcontainers base for Audit administration integration tests.
 */
public abstract class AbstractAuditHttpIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    protected WebTestClient webTestClient;
}
