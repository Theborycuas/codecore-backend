package com.codecore.iam.interfaces.http;

import com.codecore.iam.interfaces.http.dto.CreateTenantRequest;
import com.codecore.iam.interfaces.http.dto.RegisterIdentityRequest;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamUserAdminIntegrationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

@SpringBootTest(
        classes = IamUserAdminIntegrationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class BootstrapEndpointsSecurityIT extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturn401WhenCreatingTenantWithoutJwt() {
        webTestClient.post()
                .uri("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateTenantRequest("Secured Tenant " + UUID.randomUUID()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn401WhenRegisteringIdentityWithoutJwt() {
        webTestClient.post()
                .uri("/api/v1/identities")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterIdentityRequest(
                        UUID.randomUUID(),
                        "secured.%s@codecore.local".formatted(UUID.randomUUID()),
                        PASSWORD
                ))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
