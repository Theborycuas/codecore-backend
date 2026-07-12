package com.codecore.inventory.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Shared HTTP + Testcontainers base for Item administration integration tests.
 */
public abstract class AbstractItemHttpIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    protected WebTestClient webTestClient;
}
