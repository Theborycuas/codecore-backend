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
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.interfaces.http.admin.dto.ReplaceRolePermissionsRequest;
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
import java.util.List;
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
class IamRolePermissionAdminControllerIT extends AbstractPostgresIntegrationTest {

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
    void shouldAssignAndListRolePermissions() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("rp-assign");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        Role customRole = createCustomRole(tenantId, "RP_" + shortId());
        Permission read = requirePermission(IamPermissionCatalog.USER_READ);
        Permission update = requirePermission(IamPermissionCatalog.USER_UPDATE);

        webTestClient.put()
                .uri(rolePermissionsPath(customRole.id()))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReplaceRolePermissionsRequest(
                        List.of(read.id().value(), update.id().value())
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);

        webTestClient.get()
                .uri(rolePermissionsPath(customRole.id()))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    void shouldRemoveRolePermissionsOnReplace() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("rp-remove");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        Role customRole = createCustomRole(tenantId, "RM_" + shortId());
        Permission read = requirePermission(IamPermissionCatalog.USER_READ);
        Permission update = requirePermission(IamPermissionCatalog.USER_UPDATE);

        webTestClient.put()
                .uri(rolePermissionsPath(customRole.id()))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReplaceRolePermissionsRequest(
                        List.of(read.id().value(), update.id().value())
                ))
                .exchange()
                .expectStatus().isOk();

        webTestClient.put()
                .uri(rolePermissionsPath(customRole.id()))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReplaceRolePermissionsRequest(List.of(read.id().value())))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].code").isEqualTo("user:read");
    }

    @Test
    void shouldListSystemRolePermissions() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("rp-sys-get");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        Role adminRole = roleRepository
                .findByTenantIdAndCode(tenantId, SystemRoleTemplate.ADMIN.code())
                .block();
        assertThat(adminRole).isNotNull();

        webTestClient.get()
                .uri(rolePermissionsPath(adminRole.id()))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").value(count -> assertThat((Integer) count).isGreaterThan(0));
    }

    @Test
    void shouldReturn403WhenReplacingSystemRolePermissions() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("rp-sys-put");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        Role adminRole = roleRepository
                .findByTenantIdAndCode(tenantId, SystemRoleTemplate.ADMIN.code())
                .block();
        assertThat(adminRole).isNotNull();
        Permission read = requirePermission(IamPermissionCatalog.USER_READ);

        webTestClient.put()
                .uri(rolePermissionsPath(adminRole.id()))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReplaceRolePermissionsRequest(List.of(read.id().value())))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturn404ForRoleInOtherTenant() {
        TenantId tenantA = provisionTenant();
        TenantId tenantB = provisionTenant();

        String adminEmail = uniqueEmail("rp-404");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantA, adminEmail).block();
        assignSystemRole(tenantA, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantA, adminEmail);

        Role otherTenantRole = createCustomRole(tenantB, "OTH_" + shortId());

        webTestClient.get()
                .uri(rolePermissionsPath(otherTenantRole.id()))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn404ForUnknownPermissionId() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("rp-perm-404");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        Role customRole = createCustomRole(tenantId, "P404_" + shortId());

        webTestClient.put()
                .uri(rolePermissionsPath(customRole.id()))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReplaceRolePermissionsRequest(List.of(UUID.randomUUID())))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn401WithoutJwt() {
        TenantId tenantId = provisionTenant();
        Role customRole = createCustomRole(tenantId, "NOJWT_" + shortId());

        webTestClient.get()
                .uri(rolePermissionsPath(customRole.id()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn403WithoutPermissionAssign() {
        TenantId tenantId = provisionTenant();
        String email = uniqueEmail("rp-forbidden");
        IdentityTenantMembership membership = persistActiveIdentity(tenantId, email).block();
        assignSystemRole(tenantId, membership, SystemRoleTemplate.USER);
        String token = login(tenantId, email);

        Role customRole = createCustomRole(tenantId, "FB_" + shortId());

        webTestClient.get()
                .uri(rolePermissionsPath(customRole.id()))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    private String rolePermissionsPath(RoleId roleId) {
        return IamAdminApiPaths.ROLES + "/" + roleId.value() + "/permissions";
    }

    private Permission requirePermission(com.codecore.iam.domain.valueobject.PermissionCode code) {
        Permission permission = permissionRepository.findByCode(code).block();
        assertThat(permission).isNotNull();
        return permission;
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
