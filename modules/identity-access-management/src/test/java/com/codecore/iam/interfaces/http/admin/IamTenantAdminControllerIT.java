package com.codecore.iam.interfaces.http.admin;

import com.codecore.iam.application.authorization.SystemRoleTemplate;
import com.codecore.iam.application.command.CreateTenantCommand;
import com.codecore.iam.application.dto.AuthenticationResponse;
import com.codecore.iam.application.port.in.CreateTenantUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.model.membership.MembershipRoleAssignment;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.CredentialId;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantStatus;
import com.codecore.iam.interfaces.http.admin.dto.UpdateTenantRequest;
import com.codecore.iam.interfaces.http.dto.LoginRequest;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamUserAdminIntegrationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
class IamTenantAdminControllerIT extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CreateTenantUseCase createTenantUseCase;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MembershipRoleRepository membershipRoleRepository;

    @Autowired
    private com.codecore.iam.application.port.out.PasswordHasher passwordHasher;

    @Test
    void shouldGetCurrentTenantAsReadOnly() {
        TenantId tenantId = provisionTenant("Tenant-RO-" + UUID.randomUUID());
        String email = uniqueEmail("t-read");
        IdentityTenantMembership membership = persistActiveIdentity(tenantId, email).block();
        assignSystemRole(tenantId, membership, SystemRoleTemplate.READ_ONLY);
        String token = login(tenantId, email);

        webTestClient.get()
                .uri(tenantCurrentPath())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.tenantId").isEqualTo(tenantId.value().toString())
                .jsonPath("$.status").isEqualTo("ACTIVE");
    }

    @Test
    void shouldUpdateTenantAsOwner() {
        TenantId tenantId = provisionTenant("Tenant-Owner-" + UUID.randomUUID());
        String email = uniqueEmail("t-owner");
        IdentityTenantMembership membership = persistActiveIdentity(tenantId, email).block();
        assignSystemRole(tenantId, membership, SystemRoleTemplate.OWNER);
        String token = login(tenantId, email);

        String newName = "Updated Tenant " + UUID.randomUUID();

        webTestClient.put()
                .uri(tenantCurrentPath())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateTenantRequest(newName, TenantStatus.SUSPENDED))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo(newName)
                .jsonPath("$.status").isEqualTo("SUSPENDED");
    }

    @Test
    void shouldReturn403WhenAdminGetsTenant() {
        TenantId tenantId = provisionTenant("Tenant-Admin-" + UUID.randomUUID());
        String email = uniqueEmail("t-admin");
        IdentityTenantMembership membership = persistActiveIdentity(tenantId, email).block();
        assignSystemRole(tenantId, membership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, email);

        webTestClient.get()
                .uri(tenantCurrentPath())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturn403WhenReadOnlyUpdatesTenant() {
        TenantId tenantId = provisionTenant("Tenant-RO-PUT-" + UUID.randomUUID());
        String email = uniqueEmail("t-ro-put");
        IdentityTenantMembership membership = persistActiveIdentity(tenantId, email).block();
        assignSystemRole(tenantId, membership, SystemRoleTemplate.READ_ONLY);
        String token = login(tenantId, email);

        webTestClient.put()
                .uri(tenantCurrentPath())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateTenantRequest("New Name", TenantStatus.ACTIVE))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturn401WithoutJwt() {
        webTestClient.get()
                .uri(tenantCurrentPath())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn409WhenDuplicateTenantName() {
        String takenName = "Taken-" + UUID.randomUUID();
        TenantId otherTenantId = provisionTenant(takenName);
        assertThat(otherTenantId).isNotNull();

        TenantId tenantId = provisionTenant("Tenant-Dup-" + UUID.randomUUID());
        String email = uniqueEmail("t-dup");
        IdentityTenantMembership membership = persistActiveIdentity(tenantId, email).block();
        assignSystemRole(tenantId, membership, SystemRoleTemplate.OWNER);
        String token = login(tenantId, email);

        webTestClient.put()
                .uri(tenantCurrentPath())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateTenantRequest(takenName, null))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    private String tenantCurrentPath() {
        return IamAdminApiPaths.TENANTS + "/current";
    }

    private TenantId provisionTenant(String name) {
        return createTenantUseCase.execute(new CreateTenantCommand(name))
                .map(response -> {
                    assertThat(response.tenantId()).isNotNull();
                    return response.tenantId();
                })
                .block();
    }

    private void assignSystemRole(
            TenantId tenantId,
            IdentityTenantMembership membership,
            SystemRoleTemplate template
    ) {
        Role role = roleRepository.findByTenantIdAndCode(tenantId, template.code()).block();
        assertThat(role).isNotNull();
        StepVerifier.create(membershipRoleRepository.assign(
                membership.id(),
                MembershipRoleAssignment.assign(role.id(), Instant.now())
        )).verifyComplete();
    }

    private String login(TenantId tenantId, String email) {
        AuthenticationResponse response = webTestClient.post()
                .uri("/api/v1/auth/login")
                .header("X-Tenant-Id", tenantId.value().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthenticationResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(response).isNotNull();
        return response.accessToken();
    }

    private reactor.core.publisher.Mono<IdentityTenantMembership> persistActiveIdentity(
            TenantId tenantId,
            String email
    ) {
        IdentityId identityId = IdentityId.generate();
        Instant now = Instant.now();
        Identity identity = new Identity(
                identityId,
                tenantId,
                EmailAddress.of(email),
                IdentityStatus.ACTIVE,
                new Credential(
                        new CredentialId(identityId.value()),
                        PasswordHash.ofHashedValue(passwordHasher.hash(PASSWORD)),
                        now,
                        null,
                        false,
                        0L
                ),
                null,
                now,
                now,
                0L
        );
        return identityRepository.save(identity)
                .flatMap(saved -> membershipRepository.save(
                        IdentityTenantMembership.create(saved.id(), saved.tenantId(), Instant.now())
                ));
    }

    private static String uniqueEmail(String prefix) {
        return "%s.%s@codecore.local".formatted(prefix, UUID.randomUUID().toString().substring(0, 8));
    }
}
