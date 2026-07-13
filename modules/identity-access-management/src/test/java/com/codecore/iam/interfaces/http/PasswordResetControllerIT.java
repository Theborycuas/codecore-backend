package com.codecore.iam.interfaces.http;

import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.application.port.out.PasswordResetRepository;
import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.model.passwordreset.PasswordResetRequest;
import com.codecore.iam.domain.model.tenant.Tenant;
import com.codecore.iam.domain.valueobject.CredentialId;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.ResetTokenHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantName;
import com.codecore.iam.domain.valueobject.TokenExpiration;
import com.codecore.iam.infrastructure.security.Sha256TokenHasher;
import com.codecore.iam.interfaces.http.dto.ForgotPasswordRequest;
import com.codecore.iam.interfaces.http.dto.LoginRequest;
import com.codecore.iam.interfaces.http.dto.ResetPasswordRequest;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamPasswordResetHttpIntegrationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@SpringBootTest(
        classes = IamPasswordResetHttpIntegrationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class PasswordResetControllerIT extends AbstractPostgresIntegrationTest {

    private static final String OLD_PASSWORD = "ValidPass1!";
    private static final String NEW_PASSWORD = "NewValidPass2!";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void shouldReturn204OnForgotPasswordEvenWhenEmailUnknown() {
        webTestClient.post()
                .uri("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ForgotPasswordRequest(
                        "missing.%s@codecore.local".formatted(UUID.randomUUID()),
                        UUID.randomUUID()
                ))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void shouldReturn204OnForgotPasswordWhenIdentityExists() {
        TenantId tenantId = TenantId.generate();
        persistTenant(tenantId).block();
        String email = "forgot.ok.%s@codecore.local".formatted(tenantId.value());
        persistIdentity(tenantId, email, OLD_PASSWORD).block();

        webTestClient.post()
                .uri("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ForgotPasswordRequest(email, tenantId.value()))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void shouldReturn400WhenResetTokenInvalid() {
        webTestClient.post()
                .uri("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ResetPasswordRequest(
                        Sha256TokenHasher.generateRawToken(),
                        NEW_PASSWORD,
                        UUID.randomUUID()
                ))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldResetPasswordAndAllowLoginWithNewPassword() {
        TenantId tenantId = TenantId.generate();
        persistTenant(tenantId).block();
        String email = "reset.ok.%s@codecore.local".formatted(tenantId.value());
        Identity identity = persistIdentity(tenantId, email, OLD_PASSWORD).block();

        String rawToken = Sha256TokenHasher.generateRawToken();
        Instant now = Instant.now();
        PasswordResetRequest resetRequest = PasswordResetRequest.create(
                tenantId,
                identity.id(),
                ResetTokenHash.ofHashedValue(Sha256TokenHasher.hash(rawToken)),
                TokenExpiration.at(now.plus(Duration.ofHours(1))),
                now
        );
        passwordResetRepository.save(resetRequest).block();

        webTestClient.post()
                .uri("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ResetPasswordRequest(rawToken, NEW_PASSWORD, tenantId.value()))
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Tenant-Id", tenantId.value().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, OLD_PASSWORD))
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Tenant-Id", tenantId.value().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, NEW_PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty();
    }

    private reactor.core.publisher.Mono<Identity> persistIdentity(
            TenantId tenantId,
            String email,
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
                false,
                0L
        );
        Identity identity = new Identity(
                identityId,
                tenantId,
                EmailAddress.of(email),
                IdentityStatus.ACTIVE,
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
                TenantName.of("Reset-IT-" + tenantId.value()),
                now
        ));
    }
}
