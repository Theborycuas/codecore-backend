package com.codecore.organization.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * HTTP ITs use Spring Boot {@link WebTestClient} (in-process mock handler).
 */
public abstract class AbstractOrgHttpIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    protected WebTestClient webTestClient;
}
