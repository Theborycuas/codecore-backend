package com.codecore.iam.interfaces.http.admin;

import com.codecore.iam.application.authorization.IamPermissionCatalog;
import com.codecore.iam.application.authorization.SystemRoleTemplate;
import com.codecore.iam.application.command.CreateTenantCommand;
import com.codecore.iam.application.dto.AuthenticationResponse;
import com.codecore.iam.application.port.in.CreateTenantUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.configuration.IamOpenApiConfiguration;
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
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantStatus;
import com.codecore.iam.interfaces.http.admin.dto.CreateRoleRequest;
import com.codecore.iam.interfaces.http.admin.dto.CreateUserRequest;
import com.codecore.iam.interfaces.http.admin.dto.ReplaceMembershipRolesRequest;
import com.codecore.iam.interfaces.http.admin.dto.ReplaceRolePermissionsRequest;
import com.codecore.iam.interfaces.http.admin.dto.UpdateRoleRequest;
import com.codecore.iam.interfaces.http.admin.dto.UpdateTenantRequest;
import com.codecore.iam.interfaces.http.admin.dto.UpdateUserRequest;
import com.codecore.iam.interfaces.http.admin.dto.UserResponse;
import com.codecore.iam.interfaces.http.dto.CreateTenantRequest;
import com.codecore.iam.interfaces.http.dto.LoginRequest;
import com.codecore.iam.interfaces.http.dto.RegisterIdentityRequest;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamAdministrationVerificationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FASE 15.9 — end-to-end IAM administration verification (cierre FASE 15).
 */
@SpringBootTest(
        classes = IamAdministrationVerificationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class IamAdministrationVerificationIT extends AbstractPostgresIntegrationTest {

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
    private PasswordHasher passwordHasher;

    @Test
    void verification1_fullAdministrationJourney() {
        TenantId tenantId = provisionTenant();
        String ownerEmail = uniqueEmail("e2e-owner");
        IdentityTenantMembership ownerMembership = persistActiveIdentity(tenantId, ownerEmail).block();
        assignSystemRole(tenantId, ownerMembership, SystemRoleTemplate.OWNER);
        String ownerToken = login(tenantId, ownerEmail);

        webTestClient.get()
                .uri(IamAdminApiPaths.ADMINISTRATION + "/status")
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.administration").isEqualTo("operational");

        String tenantName = "E2E Tenant " + UUID.randomUUID();
        webTestClient.put()
                .uri(IamAdminApiPaths.TENANTS + "/current")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateTenantRequest(tenantName, TenantStatus.ACTIVE))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo(tenantName);

        String newUserEmail = uniqueEmail("e2e-created");
        UserResponse createdUser = webTestClient.post()
                .uri(IamAdminApiPaths.USERS)
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateUserRequest(newUserEmail, PASSWORD, IdentityStatus.ACTIVE))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UserResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(createdUser).isNotNull();

        webTestClient.get()
                .uri(IamAdminApiPaths.USERS + "/" + createdUser.id())
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo(newUserEmail.toLowerCase());

        IdentityTenantMembership targetMembership = membershipRepository
                .findByIdentityIdAndTenantId(new IdentityId(createdUser.id()), tenantId)
                .block();
        assertThat(targetMembership).isNotNull();

        Role userRole = roleRepository.findByTenantIdAndCode(tenantId, SystemRoleTemplate.USER.code()).block();
        assertThat(userRole).isNotNull();

        webTestClient.put()
                .uri(IamAdminApiPaths.MEMBERSHIPS + "/" + targetMembership.id().value() + "/roles")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReplaceMembershipRolesRequest(List.of(userRole.id().value())))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].code").isEqualTo("USER");

        String customRoleCode = "E2E_" + shortId();
        byte[] customRoleBody = webTestClient.post()
                .uri(IamAdminApiPaths.ROLES)
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateRoleRequest(customRoleCode, "E2E Custom Role"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .returnResult()
                .getResponseBody();
        assertThat(customRoleBody).isNotNull();
        UUID customRoleUuid = extractUuidFromJson(new String(customRoleBody), "id");
        Permission userRead = permissionRepository.findByCode(IamPermissionCatalog.USER_READ).block();
        assertThat(userRead).isNotNull();

        webTestClient.put()
                .uri(IamAdminApiPaths.ROLES + "/" + customRoleUuid + "/permissions")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReplaceRolePermissionsRequest(List.of(userRead.id().value())))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].code").isEqualTo("user:read");

        webTestClient.get()
                .uri(IamAdminApiPaths.PERMISSIONS + "?page=0&size=5&sort=code,asc")
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").value(count -> assertThat((Integer) count).isGreaterThanOrEqualTo(1));

        webTestClient.get()
                .uri(IamAdminApiPaths.MEMBERSHIPS + "?page=0&size=20")
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").value(count -> assertThat((Integer) count).isGreaterThanOrEqualTo(2));
    }

    @Test
    void verification2_rbacDeniesWithoutPermission() {
        TenantId tenantId = provisionTenant();
        String readerEmail = uniqueEmail("e2e-readonly");
        IdentityTenantMembership readerMembership = persistActiveIdentity(tenantId, readerEmail).block();
        assignSystemRole(tenantId, readerMembership, SystemRoleTemplate.READ_ONLY);
        String readerToken = login(tenantId, readerEmail);

        webTestClient.post()
                .uri(IamAdminApiPaths.USERS)
                .header("Authorization", "Bearer " + readerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateUserRequest(uniqueEmail("denied"), PASSWORD, IdentityStatus.ACTIVE))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void verification3_crossTenantAccessReturns404() {
        TenantId tenantA = provisionTenant();
        TenantId tenantB = provisionTenant();

        String adminEmail = uniqueEmail("e2e-tenant-a");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantA, adminEmail).block();
        assignSystemRole(tenantA, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantA, adminEmail);

        IdentityId otherTenantUserId = persistActiveIdentity(tenantB, uniqueEmail("e2e-tenant-b")).block().identityId();

        webTestClient.get()
                .uri(IamAdminApiPaths.USERS + "/" + otherTenantUserId.value())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void verification4_ownershipPolicyBlocksAdminModifyingOwner() {
        TenantId tenantId = provisionTenant();
        IdentityTenantMembership ownerMembership = persistActiveIdentity(tenantId, uniqueEmail("e2e-owner-target")).block();
        assignSystemRole(tenantId, ownerMembership, SystemRoleTemplate.OWNER);

        String adminEmail = uniqueEmail("e2e-admin-owner");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String adminToken = login(tenantId, adminEmail);

        webTestClient.put()
                .uri(IamAdminApiPaths.USERS + "/" + ownerMembership.identityId().value())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateUserRequest(IdentityStatus.LOCKED, null))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void verification5_bootstrapEndpointsRequireJwt() {
        webTestClient.post()
                .uri("/api/v1/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateTenantRequest("Secured " + UUID.randomUUID()))
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.post()
                .uri("/api/v1/identities")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterIdentityRequest(
                        UUID.randomUUID(),
                        uniqueEmail("bootstrap-denied"),
                        PASSWORD
                ))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void verification6_systemRoleIsImmutableOnPut() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("e2e-sysrole");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        Role adminRole = roleRepository.findByTenantIdAndCode(tenantId, SystemRoleTemplate.ADMIN.code()).block();
        assertThat(adminRole).isNotNull();

        webTestClient.put()
                .uri(IamAdminApiPaths.ROLES + "/" + adminRole.id().value())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateRoleRequest("Renamed Admin", null))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void verification7_openApiDocumentsAdministrationSurface() {
        webTestClient.get()
                .uri("/v3/api-docs/" + IamOpenApiConfiguration.IAM_ADMINISTRATION_GROUP)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['" + IamAdminApiPaths.USERS + "']").exists()
                .jsonPath("$.paths['" + IamAdminApiPaths.TENANTS + "/current']").exists();
    }

    @Test
    void verification8_unauthenticatedAdminRequestsReturn401() {
        webTestClient.get()
                .uri(IamAdminApiPaths.USERS)
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.get()
                .uri(IamAdminApiPaths.ADMINISTRATION + "/status")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private static UUID extractUuidFromJson(String json, String field) {
        int fieldIndex = json.indexOf("\"" + field + "\"");
        assertThat(fieldIndex).isGreaterThanOrEqualTo(0);
        int start = json.indexOf('"', fieldIndex + field.length() + 3) + 1;
        int end = json.indexOf('"', start);
        return UUID.fromString(json.substring(start, end));
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

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
