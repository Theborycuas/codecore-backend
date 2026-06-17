package com.codecore.iam.interfaces.http.admin;

import com.codecore.iam.application.authorization.IamPermissionCatalog;
import com.codecore.iam.application.authorization.SystemRoleTemplate;
import com.codecore.iam.application.command.CreateTenantCommand;
import com.codecore.iam.application.dto.AuthenticationResponse;
import com.codecore.iam.application.port.in.CreateTenantUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.model.membership.MembershipRoleAssignment;
import com.codecore.iam.domain.model.permission.Permission;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.CredentialId;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.PermissionId;
import com.codecore.iam.domain.valueobject.TenantId;
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
class IamPermissionAdminControllerIT extends AbstractPostgresIntegrationTest {

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
    private PermissionRepository permissionRepository;

    @Autowired
    private com.codecore.iam.application.port.out.PasswordHasher passwordHasher;

    @Test
    void shouldListPermissionsWhenCallerHasPermissionRead() {
        TenantId tenantId = provisionTenant();
        String ownerEmail = uniqueEmail("p-list-owner");
        IdentityTenantMembership ownerMembership = persistActiveIdentity(tenantId, ownerEmail).block();
        assignSystemRole(tenantId, ownerMembership, SystemRoleTemplate.OWNER);
        String token = login(tenantId, ownerEmail);

        webTestClient.get()
                .uri(IamAdminApiPaths.PERMISSIONS + "?page=0&size=50&sort=code,asc")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()")
                .value(count -> assertThat((Integer) count).isGreaterThanOrEqualTo(IamPermissionCatalog.ALL.size()))
                .jsonPath("$.content[0].code").exists()
                .jsonPath("$.content[0].systemPermission").isEqualTo(true);
    }

    @Test
    void shouldGetPermissionById() {
        TenantId tenantId = provisionTenant();
        String ownerEmail = uniqueEmail("p-get-owner");
        IdentityTenantMembership ownerMembership = persistActiveIdentity(tenantId, ownerEmail).block();
        assignSystemRole(tenantId, ownerMembership, SystemRoleTemplate.OWNER);
        String token = login(tenantId, ownerEmail);

        Permission permission = permissionRepository
                .findByCode(IamPermissionCatalog.USER_READ)
                .block();
        assertThat(permission).isNotNull();

        webTestClient.get()
                .uri(IamAdminApiPaths.PERMISSIONS + "/" + permission.id().value())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("user:read")
                .jsonPath("$.systemPermission").isEqualTo(true);
    }

    @Test
    void shouldReturn401WithoutJwt() {
        webTestClient.get()
                .uri(IamAdminApiPaths.PERMISSIONS)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn403WithoutPermissionRead() {
        TenantId tenantId = provisionTenant();
        String email = uniqueEmail("p-forbidden");
        IdentityTenantMembership membership = persistActiveIdentity(tenantId, email).block();
        assignSystemRole(tenantId, membership, SystemRoleTemplate.READ_ONLY);
        String token = login(tenantId, email);

        webTestClient.get()
                .uri(IamAdminApiPaths.PERMISSIONS)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturn404ForUnknownPermissionId() {
        TenantId tenantId = provisionTenant();
        String ownerEmail = uniqueEmail("p-404-owner");
        IdentityTenantMembership ownerMembership = persistActiveIdentity(tenantId, ownerEmail).block();
        assignSystemRole(tenantId, ownerMembership, SystemRoleTemplate.OWNER);
        String token = login(tenantId, ownerEmail);

        webTestClient.get()
                .uri(IamAdminApiPaths.PERMISSIONS + "/" + UUID.randomUUID())
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
