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
import com.codecore.iam.configuration.IamOpenApiConfiguration;
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
import com.codecore.organization.configuration.OrgOpenApiConfiguration;
import com.codecore.organization.interfaces.http.admin.dto.CreateOfficeRequest;
import com.codecore.organization.interfaces.http.admin.dto.CreateOrganizationRequest;
import com.codecore.organization.interfaces.http.admin.dto.CreateStaffAssignmentRequest;
import com.codecore.organization.testsupport.AbstractOrgHttpIntegrationTest;
import com.codecore.organization.testsupport.OrganizationAdministrationVerificationTestConfiguration;
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

/**
 * FASE 16.9 — end-to-end Organization Management verification (cierre FASE 16).
 */
@SpringBootTest(
        classes = OrganizationAdministrationVerificationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class OrganizationVerificationIT extends AbstractOrgHttpIntegrationTest {

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
    void verification1_fullOrganizationManagementJourney() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);

        String orgNorth = createOrganization(adminToken, "DENTAL_NORTE", "Dental Norte");
        String orgCenter = createOrganization(adminToken, "DENTAL_CENTRO", "Dental Centro");
        createOrganization(adminToken, "DENTAL_SUR", "Dental Sur");

        webTestClient.get()
                .uri(OrgAdminApiPaths.ORGANIZATIONS + "?page=0&size=20")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(3);

        AtomicReference<String> officeIdRef = new AtomicReference<>();
        webTestClient.post()
                .uri(OrgAdminApiPaths.OFFICES)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateOfficeRequest(UUID.fromString(orgNorth), "CONSULT_1", "Consultorio 1"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").value(id -> officeIdRef.set(id.toString()));

        webTestClient.get()
                .uri(OrgAdminApiPaths.OFFICES + "?organizationId=" + orgNorth)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1);

        IdentityTenantMembership staffMembership = persistActiveIdentity(tenantId, uniqueEmail("staff")).block();
        assertThat(staffMembership).isNotNull();

        AtomicReference<String> assignmentIdRef = new AtomicReference<>();
        webTestClient.post()
                .uri(OrgAdminApiPaths.STAFF_ASSIGNMENTS)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateStaffAssignmentRequest(
                        staffMembership.id().value(),
                        UUID.fromString(orgCenter),
                        null
                ))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.organizationId").isEqualTo(orgCenter)
                .jsonPath("$.id").value(id -> assignmentIdRef.set(id.toString()));

        webTestClient.get()
                .uri(OrgAdminApiPaths.STAFF_ASSIGNMENTS + "?organizationId=" + orgCenter)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1);

        webTestClient.post()
                .uri(OrgAdminApiPaths.OFFICES + "/" + officeIdRef.get() + "/archive")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri(OrgAdminApiPaths.ORGANIZATIONS + "/" + orgNorth + "/archive")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ARCHIVED");

        webTestClient.delete()
                .uri(OrgAdminApiPaths.STAFF_ASSIGNMENTS + "/" + assignmentIdRef.get())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void verification2_rbacDeniesWithoutPermission() {
        TenantId tenantId = provisionTenant();
        String readerToken = loginAs(tenantId, SystemRoleTemplate.READ_ONLY);

        webTestClient.post()
                .uri(OrgAdminApiPaths.ORGANIZATIONS)
                .header("Authorization", "Bearer " + readerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateOrganizationRequest("DENIED", "Denied"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void verification3_crossTenantAccessReturns404() {
        TenantId tenantA = provisionTenant();
        TenantId tenantB = provisionTenant();

        String adminA = loginAs(tenantA, SystemRoleTemplate.ADMIN);
        String adminB = loginAs(tenantB, SystemRoleTemplate.ADMIN);
        String orgInB = createOrganization(adminB, "TENANT_B", "Tenant B Org");

        webTestClient.get()
                .uri(OrgAdminApiPaths.ORGANIZATIONS + "/" + orgInB)
                .header("Authorization", "Bearer " + adminA)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void verification4_orgArchiveBlockedWhenActiveOfficesExist() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String orgId = createOrganization(token, "WITH_OFFICE", "With Office");

        webTestClient.post()
                .uri(OrgAdminApiPaths.OFFICES)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateOfficeRequest(UUID.fromString(orgId), "ACTIVE_OFF", "Active Office"))
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri(OrgAdminApiPaths.ORGANIZATIONS + "/" + orgId + "/archive")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void verification5_managerCanReadButNotArchive() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String managerToken = loginAs(tenantId, SystemRoleTemplate.MANAGER);
        String orgId = createOrganization(adminToken, "MANAGER_TEST", "Manager Test");

        webTestClient.get()
                .uri(OrgAdminApiPaths.ORGANIZATIONS + "/" + orgId)
                .header("Authorization", "Bearer " + managerToken)
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri(OrgAdminApiPaths.ORGANIZATIONS + "/" + orgId + "/archive")
                .header("Authorization", "Bearer " + managerToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void verification6_staffAssignmentDuplicateScopeReturns409() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String orgId = createOrganization(token, "DUP_SCOPE", "Dup Scope");
        IdentityTenantMembership membership = persistActiveIdentity(tenantId, uniqueEmail("dup")).block();
        assertThat(membership).isNotNull();

        CreateStaffAssignmentRequest request = new CreateStaffAssignmentRequest(
                membership.id().value(),
                UUID.fromString(orgId),
                null
        );

        webTestClient.post()
                .uri(OrgAdminApiPaths.STAFF_ASSIGNMENTS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri(OrgAdminApiPaths.STAFF_ASSIGNMENTS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void verification7_openApiDocumentsOrgAdministrationSurface() {
        webTestClient.get()
                .uri("/v3/api-docs/" + OrgOpenApiConfiguration.ORG_ADMINISTRATION_GROUP)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['" + OrgAdminApiPaths.ORGANIZATIONS + "']").exists()
                .jsonPath("$.paths['" + OrgAdminApiPaths.OFFICES + "']").exists()
                .jsonPath("$.paths['" + OrgAdminApiPaths.STAFF_ASSIGNMENTS + "']").exists();
    }

    @Test
    void verification8_unauthenticatedRequestsReturn401() {
        webTestClient.get()
                .uri(OrgAdminApiPaths.ORGANIZATIONS)
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.get()
                .uri(OrgAdminApiPaths.OFFICES + "?organizationId=" + UUID.randomUUID())
                .exchange()
                .expectStatus().isUnauthorized();
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
