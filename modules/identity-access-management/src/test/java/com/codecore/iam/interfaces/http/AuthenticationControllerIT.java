package com.codecore.iam.interfaces.http;

import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.tenant.Tenant;
import com.codecore.iam.domain.valueobject.CredentialId;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantName;
import com.codecore.iam.interfaces.http.dto.LoginRequest;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamLoginHttpIntegrationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.UUID;

@SpringBootTest(
        classes = IamLoginHttpIntegrationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class AuthenticationControllerIT extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void shouldReturn200WithAccessTokenWhenCredentialsValid() {
        TenantId tenantId = TenantId.generate();
        persistTenant(tenantId).block();
        String email = "login.ok.%s@codecore.local".formatted(tenantId.value());
        persistIdentity(tenantId, email, IdentityStatus.ACTIVE, PASSWORD).block();

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Tenant-Id", tenantId.value().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.expiresIn").isEqualTo(900);
    }

    @Test
    void shouldReturn401WhenPasswordIncorrect() {
        TenantId tenantId = TenantId.generate();
        String email = "login.wrong.%s@codecore.local".formatted(tenantId.value());
        persistIdentity(tenantId, email, IdentityStatus.ACTIVE, PASSWORD).block();

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Tenant-Id", tenantId.value().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, "WrongPass1!"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn401WhenUserDoesNotExist() {
        UUID tenantId = UUID.randomUUID();

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Tenant-Id", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("missing.%s@codecore.local".formatted(tenantId), PASSWORD))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn423WhenAccountLocked() {
        TenantId tenantId = TenantId.generate();
        String email = "login.locked.%s@codecore.local".formatted(tenantId.value());
        persistIdentity(tenantId, email, IdentityStatus.LOCKED, PASSWORD).block();

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Tenant-Id", tenantId.value().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, PASSWORD))
                .exchange()
                .expectStatus().isEqualTo(423);
    }

    @Test
    void shouldReturn403WhenAccountDisabled() {
        TenantId tenantId = TenantId.generate();
        String email = "login.disabled.%s@codecore.local".formatted(tenantId.value());
        persistIdentity(tenantId, email, IdentityStatus.DISABLED, PASSWORD).block();

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Tenant-Id", tenantId.value().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, PASSWORD))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturn400WhenPayloadInvalid() {
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Tenant-Id", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"email": "", "password": "%s"}
                        """.formatted(PASSWORD))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn403WhenMembershipMissing() {
        TenantId tenantId = TenantId.generate();
        String email = "http.no.membership.%s@codecore.local".formatted(tenantId.value());
        persistIdentityWithoutMembership(tenantId, email, IdentityStatus.ACTIVE, PASSWORD).block();

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Tenant-Id", tenantId.value().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, PASSWORD))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturn400WhenTenantHeaderMissing() {
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("user@codecore.local", PASSWORD))
                .exchange()
                .expectStatus().isBadRequest();
    }

    private reactor.core.publisher.Mono<Identity> persistIdentity(
            TenantId tenantId,
            String email,
            IdentityStatus status,
            String rawPassword
    ) {
        IdentityId identityId = IdentityId.generate();
        Instant now = Instant.now();
        String hashed = passwordHasher.hash(rawPassword);
        Credential credential = new Credential(
                new CredentialId(identityId.value()),
                PasswordHash.ofHashedValue(hashed),
                now,
                null,
                status == IdentityStatus.PASSWORD_RESET_REQUIRED,
                0L
        );
        Identity identity = new Identity(
                identityId,
                tenantId,
                EmailAddress.of(email),
                status,
                credential,
                null,
                now,
                now,
                0L
        );
        return identityRepository.save(identity)
                .flatMap(saved -> {
                    IdentityTenantMembership membership = IdentityTenantMembership.create(
                            saved.id(),
                            saved.tenantId(),
                            Instant.now()
                    );
                    return membershipRepository.save(membership).thenReturn(saved);
                });
    }

    private reactor.core.publisher.Mono<Tenant> persistTenant(TenantId tenantId) {
        Instant now = Instant.now();
        return tenantRepository.save(Tenant.create(
                tenantId,
                TenantName.of("Login-IT-" + tenantId.value()),
                now
        ));
    }

    private reactor.core.publisher.Mono<Identity> persistIdentityWithoutMembership(
            TenantId tenantId,
            String email,
            IdentityStatus status,
            String rawPassword
    ) {
        IdentityId identityId = IdentityId.generate();
        Instant now = Instant.now();
        String hashed = passwordHasher.hash(rawPassword);
        Credential credential = new Credential(
                new CredentialId(identityId.value()),
                PasswordHash.ofHashedValue(hashed),
                now,
                null,
                status == IdentityStatus.PASSWORD_RESET_REQUIRED,
                0L
        );
        Identity identity = new Identity(
                identityId,
                tenantId,
                EmailAddress.of(email),
                status,
                credential,
                null,
                now,
                now,
                0L
        );
        return identityRepository.save(identity);
    }
}
