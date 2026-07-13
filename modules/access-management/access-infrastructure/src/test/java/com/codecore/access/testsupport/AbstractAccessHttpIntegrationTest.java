package com.codecore.access.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Shared HTTP + Testcontainers base for Access administration integration tests.
 */
public abstract class AbstractAccessHttpIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    protected WebTestClient webTestClient;
}
