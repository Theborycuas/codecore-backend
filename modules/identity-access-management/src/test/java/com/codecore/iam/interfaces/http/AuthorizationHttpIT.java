package com.codecore.iam.interfaces.http;

import com.codecore.iam.application.dto.AuthenticationResponse;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.application.port.out.RolePermissionRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.model.membership.MembershipRoleAssignment;
import com.codecore.iam.domain.model.permission.Permission;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.model.role.RolePermissionAssignment;
import com.codecore.iam.domain.model.tenant.Tenant;
import com.codecore.iam.domain.valueobject.CredentialId;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleName;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantName;
import com.codecore.iam.interfaces.http.dto.LoginRequest;
import com.codecore.iam.interfaces.http.AuthorizationProbeController;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamAuthorizationHttpIntegrationTestConfiguration;
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

@SpringBootTest(
        classes = IamAuthorizationHttpIntegrationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class AuthorizationHttpIT extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private MembershipRoleRepository membershipRoleRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void shouldReturn200WhenMembershipHasRequiredPermission() {
        TenantId tenantId = TenantId.generate();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "authz.ok.%s@codecore.local".formatted(suffix);
        persistActiveTenant(tenantId).block();
        IdentityTenantMembership membership = persistIdentityWithMembership(tenantId, email).block();
        grantProbePermission(tenantId, suffix, membership, Instant.now());

        String accessToken = loginAndExtractToken(tenantId, email);

        webTestClient.get()
                .uri("/api/v1/auth/authorization-probe")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.authorized").isEqualTo("true");
    }

    @Test
    void shouldReturn403WhenMembershipLacksRequiredPermission() {
        TenantId tenantId = TenantId.generate();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "authz.denied.%s@codecore.local".formatted(suffix);
        persistActiveTenant(tenantId).block();
        persistIdentityWithMembership(tenantId, email).block();

        String accessToken = loginAndExtractToken(tenantId, email);

        webTestClient.get()
                .uri("/api/v1/auth/authorization-probe")
                .header("Authorization", "Bearer " + accessToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    private reactor.core.publisher.Mono<Tenant> persistActiveTenant(TenantId tenantId) {
        return tenantRepository.save(Tenant.create(tenantId, TenantName.of("Tenant " + tenantId.value()), Instant.now()));
    }

    private void grantProbePermission(
            TenantId tenantId,
            String suffix,
            IdentityTenantMembership membership,
            Instant now
    ) {
        Role role = Role.create(tenantId, RoleCode.of("PROBE_" + suffix), RoleName.of("Probe role"), now);
        StepVerifier.create(roleRepository.save(role)).expectNextCount(1).verifyComplete();

        Permission permission = Permission.create(
                PermissionCode.of(AuthorizationProbeController.PROBE_PERMISSION),
                "probe permission",
                now
        );
        StepVerifier.create(permissionRepository.save(permission)).expectNextCount(1).verifyComplete();

        StepVerifier.create(rolePermissionRepository.assign(
                role.id(),
                RolePermissionAssignment.assign(permission.id(), now)
        )).verifyComplete();

        StepVerifier.create(membershipRoleRepository.assign(
                membership.id(),
                MembershipRoleAssignment.assign(role.id(), now)
        )).verifyComplete();
    }

    private String loginAndExtractToken(TenantId tenantId, String email) {
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
        return response.accessToken();
    }

    private reactor.core.publisher.Mono<IdentityTenantMembership> persistIdentityWithMembership(
            TenantId tenantId,
            String email
    ) {
        IdentityId identityId = IdentityId.generate();
        Instant now = Instant.now();
        String hashed = passwordHasher.hash(PASSWORD);
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
                    return membershipRepository.save(membership);
                });
    }
}
