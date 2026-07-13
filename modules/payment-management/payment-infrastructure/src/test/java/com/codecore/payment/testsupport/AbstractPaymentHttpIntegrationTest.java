package com.codecore.payment.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Shared HTTP + Testcontainers base for Payment administration integration tests.
 */
public abstract class AbstractPaymentHttpIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    protected WebTestClient webTestClient;
}
