package com.codecore.appointment.testsupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Shared HTTP + Testcontainers base for Appointment administration integration tests.
 */
public abstract class AbstractAppointmentHttpIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    protected WebTestClient webTestClient;
}
