package com.codecore.organization.interfaces.http.admin;

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
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.interfaces.http.dto.LoginRequest;
import com.codecore.organization.interfaces.http.admin.dto.CreateOrganizationRequest;
import com.codecore.organization.interfaces.http.admin.dto.UpdateOrganizationRequest;
import com.codecore.organization.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.organization.testsupport.OrgAdminIntegrationTestConfiguration;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = OrgAdminIntegrationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class OrganizationAdminControllerIT extends AbstractPostgresIntegrationTest {

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
    void shouldCreateAndListOrganizationsWhenAdmin() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);

        webTestClient.post()
                .uri(OrgAdminApiPaths.ORGANIZATIONS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateOrganizationRequest("DENTAL_NORTE", "Dental Norte"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.code").isEqualTo("DENTAL_NORTE")
                .jsonPath("$.status").isEqualTo("ACTIVE");

        webTestClient.get()
                .uri(OrgAdminApiPaths.ORGANIZATIONS + "?page=0&size=20")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").value(count -> assertThat((Integer) count).isGreaterThanOrEqualTo(1));
    }

    @Test
    void shouldArchiveOrganizationWhenAdmin() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String orgId = createOrganization(token, "ARCHIVE_ME", "Archive Me");

        webTestClient.post()
                .uri(OrgAdminApiPaths.ORGANIZATIONS + "/" + orgId + "/archive")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ARCHIVED");
    }

    @Test
    void shouldReturn403WhenManagerTriesToArchive() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String managerToken = loginAs(tenantId, SystemRoleTemplate.MANAGER);
        String orgId = createOrganization(adminToken, "NO_ARCHIVE", "No Archive");

        webTestClient.post()
                .uri(OrgAdminApiPaths.ORGANIZATIONS + "/" + orgId + "/archive")
                .header("Authorization", "Bearer " + managerToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturn404ForCrossTenantOrganization() {
        TenantId tenantA = provisionTenant();
        TenantId tenantB = provisionTenant();
        String adminA = loginAs(tenantA, SystemRoleTemplate.ADMIN);
        String adminB = loginAs(tenantB, SystemRoleTemplate.ADMIN);
        String orgIdInB = createOrganization(adminB, "TENANT_B", "Tenant B Org");

        webTestClient.get()
                .uri(OrgAdminApiPaths.ORGANIZATIONS + "/" + orgIdInB)
                .header("Authorization", "Bearer " + adminA)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldListArchivedWhenManagerRequestsFilter() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String managerToken = loginAs(tenantId, SystemRoleTemplate.MANAGER);
        String orgId = createOrganization(adminToken, "ARCHIVED_ORG", "Archived Org");

        webTestClient.post()
                .uri(OrgAdminApiPaths.ORGANIZATIONS + "/" + orgId + "/archive")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri(OrgAdminApiPaths.ORGANIZATIONS + "?status=ARCHIVED")
                .header("Authorization", "Bearer " + managerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").value(count -> assertThat((Integer) count).isGreaterThanOrEqualTo(1));
    }

    @Test
    void shouldUpdateOrganizationName() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String orgId = createOrganization(token, "RENAME_ME", "Original");

        webTestClient.put()
                .uri(OrgAdminApiPaths.ORGANIZATIONS + "/" + orgId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateOrganizationRequest("Renamed"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Renamed");
    }

    private String createOrganization(String token, String code, String name) {
        AtomicReference<String> idRef = new AtomicReference<>();
        webTestClient.post()
                .uri(OrgAdminApiPaths.ORGANIZATIONS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateOrganizationRequest(code, name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").value(id -> idRef.set(id.toString()));
        assertThat(idRef.get()).isNotNull();
        return idRef.get();
    }

    private TenantId provisionTenant() {
        return createTenantUseCase.execute(new CreateTenantCommand("Tenant-" + UUID.randomUUID()))
                .map(response -> {
                    assertThat(response.tenantId()).isNotNull();
                    return response.tenantId();
                })
                .block();
    }

    private String loginAs(TenantId tenantId, SystemRoleTemplate role) {
        String email = uniqueEmail(role.name().toLowerCase());
        IdentityTenantMembership membership = persistActiveIdentity(tenantId, email).block();
        assignSystemRole(tenantId, membership, role);
        return login(tenantId, email);
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
