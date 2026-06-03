package com.codecore.iam.interfaces.http;

import com.codecore.iam.interfaces.http.dto.CreateTenantRequest;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamTenantHttpIntegrationTestConfiguration;
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
        classes = IamTenantHttpIntegrationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
class CreateTenantControllerIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturn201AndPersistRow() throws Exception {
        String name = "PetNova Demo %s".formatted(UUID.randomUUID());

        webTestClient.post()
                .uri("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateTenantRequest(name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.tenantId").isNotEmpty()
                .jsonPath("$.name").isEqualTo(name)
                .jsonPath("$.status").isEqualTo("ACTIVE");

        try (Connection connection = openJdbcConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     SELECT status FROM iam.tenant WHERE name = ?
                     """)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("status")).isEqualTo("ACTIVE");
            }
        }
    }

    @Test
    void shouldReturn409WhenNameDuplicated() {
        String name = "Duplicate Tenant %s".formatted(UUID.randomUUID());
        CreateTenantRequest request = new CreateTenantRequest(name);

        webTestClient.post()
                .uri("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void shouldReturn400WhenNameBlank() {
        webTestClient.post()
                .uri("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": ""}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn400WhenNameMissing() {
        webTestClient.post()
                .uri("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
