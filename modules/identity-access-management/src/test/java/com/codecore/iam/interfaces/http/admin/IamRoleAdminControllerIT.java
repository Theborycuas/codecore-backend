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
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.RoleStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.interfaces.http.admin.dto.CreateRoleRequest;
import com.codecore.iam.interfaces.http.admin.dto.UpdateRoleRequest;
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
class IamRoleAdminControllerIT extends AbstractPostgresIntegrationTest {

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
    void shouldListRolesIncludingSystemRoles() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("r-list-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        webTestClient.get()
                .uri(IamAdminApiPaths.ROLES + "?page=0&size=20")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").value(count -> assertThat((Integer) count).isGreaterThanOrEqualTo(5));
    }

    @Test
    void shouldCreateCustomRole() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("r-create-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        String code = "CUSTOM_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        webTestClient.post()
                .uri(IamAdminApiPaths.ROLES)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateRoleRequest(code, "Custom Role"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.code").isEqualTo(code)
                .jsonPath("$.systemRole").isEqualTo(false);
    }

    @Test
    void shouldUpdateCustomRoleStatus() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("r-upd-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        Role customRole = createCustomRole(tenantId, "UPD_" + shortId());

        webTestClient.put()
                .uri(IamAdminApiPaths.ROLES + "/" + customRole.id().value())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateRoleRequest(null, RoleStatus.INACTIVE))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("INACTIVE");
    }

    @Test
    void shouldDeleteUnusedCustomRole() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("r-del-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        Role customRole = createCustomRole(tenantId, "DEL_" + shortId());

        webTestClient.delete()
                .uri(IamAdminApiPaths.ROLES + "/" + customRole.id().value())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        Role deleted = roleRepository.findById(customRole.id()).block();
        assertThat(deleted).isNull();
    }

    @Test
    void shouldReturn403WhenDeletingSystemRole() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("r-sys-del");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        Role adminRole = roleRepository
                .findByTenantIdAndCode(tenantId, SystemRoleTemplate.ADMIN.code())
                .block();
        assertThat(adminRole).isNotNull();

        webTestClient.delete()
                .uri(IamAdminApiPaths.ROLES + "/" + adminRole.id().value())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturn409WhenRoleIsInUse() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("r-inuse-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        Role customRole = createCustomRole(tenantId, "USE_" + shortId());
        StepVerifier.create(membershipRoleRepository.assign(
                adminMembership.id(),
                MembershipRoleAssignment.assign(customRole.id(), Instant.now())
        )).verifyComplete();

        webTestClient.delete()
                .uri(IamAdminApiPaths.ROLES + "/" + customRole.id().value())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void shouldReturn401WithoutJwt() {
        webTestClient.get()
                .uri(IamAdminApiPaths.ROLES)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn403WithoutRoleRead() {
        TenantId tenantId = provisionTenant();
        String email = uniqueEmail("r-forbidden");
        persistActiveIdentity(tenantId, email).block();
        String token = login(tenantId, email);

        webTestClient.get()
                .uri(IamAdminApiPaths.ROLES)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturn404ForRoleInOtherTenant() {
        TenantId tenantA = provisionTenant();
        TenantId tenantB = provisionTenant();

        String adminEmail = uniqueEmail("r-404-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantA, adminEmail).block();
        assignSystemRole(tenantA, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantA, adminEmail);

        Role otherTenantRole = createCustomRole(tenantB, "OTH_" + shortId());

        webTestClient.get()
                .uri(IamAdminApiPaths.ROLES + "/" + otherTenantRole.id().value())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn409WhenRoleCodeAlreadyExists() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("r-dup-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        String code = "DUP_" + shortId();
        createCustomRole(tenantId, code);

        webTestClient.post()
                .uri(IamAdminApiPaths.ROLES)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateRoleRequest(code, "Duplicate Role"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    private TenantId provisionTenant() {
        return createTenantUseCase.execute(new CreateTenantCommand("Tenant-" + UUID.randomUUID()))
                .map(response -> {
                    assertThat(response.tenantId()).isNotNull();
                    return response.tenantId();
                })
                .block();
    }

    private Role createCustomRole(TenantId tenantId, String code) {
        Instant now = Instant.now();
        Role role = Role.create(
                tenantId,
                RoleCode.of(code),
                RoleName.of("Custom " + code),
                now
        );
        return roleRepository.save(role).block();
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
