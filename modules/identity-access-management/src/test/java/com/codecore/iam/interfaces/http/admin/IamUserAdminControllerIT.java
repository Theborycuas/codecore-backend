package com.codecore.iam.interfaces.http.admin;

import com.codecore.iam.application.authorization.SystemRoleTemplate;
import com.codecore.iam.application.command.CreateTenantCommand;
import com.codecore.iam.application.dto.AuthenticationResponse;
import com.codecore.iam.application.port.in.CreateTenantUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
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
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.interfaces.http.admin.dto.CreateUserRequest;
import com.codecore.iam.interfaces.http.admin.dto.UpdateUserRequest;
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
class IamUserAdminControllerIT extends AbstractPostgresIntegrationTest {

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
    private PasswordHasher passwordHasher;

    @Test
    void shouldListUsersWhenCallerHasUserRead() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("list-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        String targetEmail = uniqueEmail("listed-user");
        persistActiveIdentity(tenantId, targetEmail).block();

        webTestClient.get()
                .uri(IamAdminApiPaths.USERS + "?page=0&size=20")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").value(count -> assertThat((Integer) count).isGreaterThanOrEqualTo(2))
                .jsonPath("$.page").isEqualTo(0);
    }

    @Test
    void shouldGetUserByIdWhenMemberOfTenant() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("get-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        String targetEmail = uniqueEmail("get-target");
        IdentityId targetId = persistActiveIdentity(tenantId, targetEmail).block().identityId();

        webTestClient.get()
                .uri(IamAdminApiPaths.USERS + "/" + targetId.value())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(targetId.value().toString())
                .jsonPath("$.email").isEqualTo(targetEmail.toLowerCase());
    }

    @Test
    void shouldCreateUserWhenCallerHasUserCreate() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("create-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        String newEmail = uniqueEmail("created");

        webTestClient.post()
                .uri(IamAdminApiPaths.USERS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateUserRequest(newEmail, PASSWORD, IdentityStatus.ACTIVE))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.email").isEqualTo(newEmail.toLowerCase())
                .jsonPath("$.status").isEqualTo("ACTIVE");
    }

    @Test
    void shouldUpdateUserStatusWhenAdminModifiesUser() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("update-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        String targetEmail = uniqueEmail("update-target");
        IdentityId targetId = persistActiveIdentity(tenantId, targetEmail).block().identityId();

        webTestClient.put()
                .uri(IamAdminApiPaths.USERS + "/" + targetId.value())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateUserRequest(IdentityStatus.LOCKED, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("LOCKED");
    }

    @Test
    void shouldDeactivateUserWithDeleteEndpoint() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("delete-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        String targetEmail = uniqueEmail("delete-target");
        IdentityId targetId = persistActiveIdentity(tenantId, targetEmail).block().identityId();

        webTestClient.delete()
                .uri(IamAdminApiPaths.USERS + "/" + targetId.value())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        Identity disabled = identityRepository.findById(targetId).block();
        assertThat(disabled).isNotNull();
        assertThat(disabled.status()).isEqualTo(IdentityStatus.ACTIVE);

        IdentityTenantMembership membership = membershipRepository
                .findByIdentityIdAndTenantId(targetId, tenantId)
                .block();
        assertThat(membership).isNotNull();
        assertThat(membership.status()).isEqualTo(MembershipStatus.INACTIVE);
    }

    @Test
    void shouldReturn401WithoutJwt() {
        webTestClient.get()
                .uri(IamAdminApiPaths.USERS)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn403WithoutUserReadPermission() {
        TenantId tenantId = provisionTenant();
        String email = uniqueEmail("forbidden");
        persistActiveIdentity(tenantId, email).block();
        String token = login(tenantId, email);

        webTestClient.get()
                .uri(IamAdminApiPaths.USERS)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturn403WhenAdminTriesToModifyOwner() {
        TenantId tenantId = provisionTenant();
        String ownerEmail = uniqueEmail("owner");
        IdentityTenantMembership ownerMembership = persistActiveIdentity(tenantId, ownerEmail).block();
        assignSystemRole(tenantId, ownerMembership, SystemRoleTemplate.OWNER);

        String adminEmail = uniqueEmail("admin-vs-owner");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String adminToken = login(tenantId, adminEmail);

        IdentityId ownerId = ownerMembership.identityId();

        webTestClient.put()
                .uri(IamAdminApiPaths.USERS + "/" + ownerId.value())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateUserRequest(IdentityStatus.LOCKED, null))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturn404WhenUserNotInTenant() {
        TenantId tenantA = provisionTenant();
        TenantId tenantB = provisionTenant();

        String adminEmail = uniqueEmail("tenant-a-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantA, adminEmail).block();
        assignSystemRole(tenantA, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantA, adminEmail);

        String otherTenantEmail = uniqueEmail("tenant-b-only");
        IdentityId otherTenantUserId = persistActiveIdentity(tenantB, otherTenantEmail).block().identityId();

        webTestClient.get()
                .uri(IamAdminApiPaths.USERS + "/" + otherTenantUserId.value())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    private TenantId provisionTenant() {
        return createTenantUseCase.execute(new CreateTenantCommand("Tenant-" + UUID.randomUUID()))
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
