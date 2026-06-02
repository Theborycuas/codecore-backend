package com.codecore.iam.interfaces.http;

import com.codecore.iam.interfaces.http.dto.RegisterIdentityRequest;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamHttpIntegrationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = IamHttpIntegrationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
class RegisterIdentityControllerIT extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturn201AndPersistRow() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String email = "http.register.%s@codecore.local".formatted(tenantId);

        webTestClient.post()
                .uri("/api/v1/identities")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterIdentityRequest(tenantId, email, PASSWORD))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.identityId").isNotEmpty()
                .jsonPath("$.tenantId").isEqualTo(tenantId.toString())
                .jsonPath("$.email").isEqualTo(email.toLowerCase())
                .jsonPath("$.status").isEqualTo("PENDING_VERIFICATION");

        try (Connection connection = openJdbcConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     SELECT status FROM iam.iam_user
                     WHERE tenant_id = ? AND normalized_email = ?
                     """)) {
            ps.setObject(1, tenantId);
            ps.setString(2, email.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("status")).isEqualTo("PENDING_VERIFICATION");
            }
        }
    }

    @Test
    void shouldReturn409WhenEmailDuplicatedInSameTenant() {
        UUID tenantId = UUID.randomUUID();
        String email = "http.duplicate.%s@codecore.local".formatted(tenantId);
        RegisterIdentityRequest request = new RegisterIdentityRequest(tenantId, email, PASSWORD);

        webTestClient.post()
                .uri("/api/v1/identities")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri("/api/v1/identities")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void shouldReturn400WhenRequiredFieldsMissing() {
        webTestClient.post()
                .uri("/api/v1/identities")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"tenantId": "%s", "email": ""}
                        """.formatted(UUID.randomUUID()))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
