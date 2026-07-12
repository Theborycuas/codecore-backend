package com.codecore.inventory.interfaces.http.admin;

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
import com.codecore.inventory.configuration.InventoryOpenApiConfiguration;
import com.codecore.inventory.interfaces.http.admin.dto.CreateItemRequest;
import com.codecore.inventory.interfaces.http.admin.dto.UpdateItemRequest;
import com.codecore.inventory.testsupport.AbstractItemHttpIntegrationTest;
import com.codecore.inventory.testsupport.ItemAdministrationVerificationTestConfiguration;
import com.codecore.organization.interfaces.http.admin.OrgAdminApiPaths;
import com.codecore.organization.interfaces.http.admin.dto.CreateOrganizationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FASE 20.7 — end-to-end Item Inventory verification (cierre slice Item).
 */
@SpringBootTest(
        classes = ItemAdministrationVerificationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class ItemVerificationIT extends AbstractItemHttpIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";

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
    void verification1_fullItemCatalogJourney() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String orgId = createOrganization(adminToken, "STOCK_CORE", "Stock Core");

        String itemWithOrg = createItem(
                adminToken,
                "Nitrile Gloves M",
                "SKU-GLOVE-M",
                UUID.fromString(orgId)
        );
        createItem(adminToken, "Composite Resin A2", null, null);
        createItem(adminToken, "Saline 500ml", "SKU-SALINE", null);

        webTestClient.get()
                .uri(ItemAdminApiPaths.ITEMS + "?page=0&size=20")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(3)
                .jsonPath("$.content[0].tenantId").doesNotExist();

        webTestClient.get()
                .uri(ItemAdminApiPaths.ITEMS + "?primaryOrganizationId=" + orgId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].id").isEqualTo(itemWithOrg);

        webTestClient.get()
                .uri(ItemAdminApiPaths.ITEMS + "?code=SKU-GLOVE-M")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].id").isEqualTo(itemWithOrg);

        webTestClient.get()
                .uri(ItemAdminApiPaths.ITEMS + "?q=Gloves")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1);

        webTestClient.put()
                .uri(ItemAdminApiPaths.ITEMS + "/" + itemWithOrg)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateItemRequest(
                        "Nitrile Gloves M (Box)",
                        "SKU-GLOVE-M",
                        UUID.fromString(orgId)
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.displayName").isEqualTo("Nitrile Gloves M (Box)");

        webTestClient.post()
                .uri(ItemAdminApiPaths.ITEMS + "/" + itemWithOrg + "/archive")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ARCHIVED");

        webTestClient.get()
                .uri(ItemAdminApiPaths.ITEMS + "?status=ACTIVE")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2);

        webTestClient.get()
                .uri(ItemAdminApiPaths.ITEMS + "/" + itemWithOrg)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ARCHIVED");

        webTestClient.post()
                .uri(ItemAdminApiPaths.ITEMS + "/" + itemWithOrg + "/activate")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ACTIVE");
    }

    @Test
    void verification2_rbacDeniesCreateWithoutPermission() {
        TenantId tenantId = provisionTenant();
        String readerToken = loginAs(tenantId, SystemRoleTemplate.READ_ONLY);

        webTestClient.post()
                .uri(ItemAdminApiPaths.ITEMS)
                .header("Authorization", "Bearer " + readerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateItemRequest("Denied", null, null))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void verification3_crossTenantAccessReturns404() {
        TenantId tenantA = provisionTenant();
        TenantId tenantB = provisionTenant();
        String adminA = loginAs(tenantA, SystemRoleTemplate.ADMIN);
        String adminB = loginAs(tenantB, SystemRoleTemplate.ADMIN);
        String itemInB = createItem(adminB, "Tenant B Item", null, null);

        webTestClient.get()
                .uri(ItemAdminApiPaths.ITEMS + "/" + itemInB)
                .header("Authorization", "Bearer " + adminA)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void verification4_invalidPrimaryOrganizationReturns404() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);

        webTestClient.post()
                .uri(ItemAdminApiPaths.ITEMS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateItemRequest(
                        "No Org",
                        null,
                        UUID.randomUUID()
                ))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void verification5_managerCanArchiveItemPerMatrix() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String managerToken = loginAs(tenantId, SystemRoleTemplate.MANAGER);
        String itemId = createItem(adminToken, "Manager Archive", null, null);

        webTestClient.get()
                .uri(ItemAdminApiPaths.ITEMS + "/" + itemId)
                .header("Authorization", "Bearer " + managerToken)
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri(ItemAdminApiPaths.ITEMS + "/" + itemId + "/archive")
                .header("Authorization", "Bearer " + managerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ARCHIVED");
    }

    @Test
    void verification6_duplicateCodeReturns409() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);

        webTestClient.post()
                .uri(ItemAdminApiPaths.ITEMS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateItemRequest("First", "DUP-SKU", null))
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri(ItemAdminApiPaths.ITEMS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateItemRequest("Second", "DUP-SKU", null))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void verification7_openApiDocumentsInventoryAdministrationSurface() {
        webTestClient.get()
                .uri("/v3/api-docs/" + InventoryOpenApiConfiguration.INVENTORY_ADMINISTRATION_GROUP)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['" + ItemAdminApiPaths.ITEMS + "']").exists();
    }

    @Test
    void verification8_unauthenticatedRequestsReturn401() {
        webTestClient.get()
                .uri(ItemAdminApiPaths.ITEMS)
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.get()
                .uri(ItemAdminApiPaths.ITEMS + "/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private String createItem(
            String token,
            String displayName,
            String code,
            UUID primaryOrganizationId
    ) {
        AtomicReference<String> idRef = new AtomicReference<>();
        webTestClient.post()
                .uri(ItemAdminApiPaths.ITEMS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateItemRequest(displayName, code, primaryOrganizationId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").value(id -> idRef.set(id.toString()));
        assertThat(idRef.get()).isNotNull();
        return idRef.get();
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
