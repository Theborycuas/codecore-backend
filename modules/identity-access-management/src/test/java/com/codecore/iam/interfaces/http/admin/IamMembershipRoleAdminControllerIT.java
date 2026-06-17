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
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.interfaces.http.admin.dto.ReplaceMembershipRolesRequest;
import com.codecore.iam.interfaces.http.admin.dto.UpdateMembershipRequest;
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
class IamMembershipRoleAdminControllerIT extends AbstractPostgresIntegrationTest {

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
    void shouldAssignAndListMembershipRoles() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("mr-assign");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        String targetEmail = uniqueEmail("mr-target");
        IdentityTenantMembership targetMembership = persistActiveIdentity(tenantId, targetEmail).block();
        Role userRole = requireSystemRole(tenantId, SystemRoleTemplate.USER);

        webTestClient.put()
                .uri(membershipRolesPath(targetMembership.id()))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReplaceMembershipRolesRequest(List.of(userRole.id().value())))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].code").isEqualTo("USER");

        webTestClient.get()
                .uri(membershipRolesPath(targetMembership.id()))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void shouldRemoveMembershipRolesOnReplace() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("mr-remove");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        String targetEmail = uniqueEmail("mr-remove-target");
        IdentityTenantMembership targetMembership = persistActiveIdentity(tenantId, targetEmail).block();
        Role userRole = requireSystemRole(tenantId, SystemRoleTemplate.USER);
        Role managerRole = requireSystemRole(tenantId, SystemRoleTemplate.MANAGER);

        webTestClient.put()
                .uri(membershipRolesPath(targetMembership.id()))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReplaceMembershipRolesRequest(List.of(
                        userRole.id().value(),
                        managerRole.id().value()
                )))
                .exchange()
                .expectStatus().isOk();

        webTestClient.put()
                .uri(membershipRolesPath(targetMembership.id()))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReplaceMembershipRolesRequest(List.of(userRole.id().value())))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].code").isEqualTo("USER");
    }

    @Test
    void shouldReturn403WhenAdminModifiesOwnerRoles() {
        TenantId tenantId = provisionTenant();
        String ownerEmail = uniqueEmail("mr-owner");
        IdentityTenantMembership ownerMembership = persistActiveIdentity(tenantId, ownerEmail).block();
        assignSystemRole(tenantId, ownerMembership, SystemRoleTemplate.OWNER);

        String adminEmail = uniqueEmail("mr-admin");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String adminToken = login(tenantId, adminEmail);

        Role userRole = requireSystemRole(tenantId, SystemRoleTemplate.USER);

        webTestClient.put()
                .uri(membershipRolesPath(ownerMembership.id()))
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReplaceMembershipRolesRequest(List.of(userRole.id().value())))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturn404ForMembershipInOtherTenant() {
        TenantId tenantA = provisionTenant();
        TenantId tenantB = provisionTenant();

        String adminEmail = uniqueEmail("mr-404");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantA, adminEmail).block();
        assignSystemRole(tenantA, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantA, adminEmail);

        IdentityTenantMembership otherTenantMembership = persistActiveIdentity(
                tenantB,
                uniqueEmail("mr-other")
        ).block();

        webTestClient.get()
                .uri(membershipRolesPath(otherTenantMembership.id()))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn404ForUnknownRoleId() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("mr-role-404");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        String targetEmail = uniqueEmail("mr-role-target");
        IdentityTenantMembership targetMembership = persistActiveIdentity(tenantId, targetEmail).block();

        webTestClient.put()
                .uri(membershipRolesPath(targetMembership.id()))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReplaceMembershipRolesRequest(List.of(UUID.randomUUID())))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn400WhenMembershipInactive() {
        TenantId tenantId = provisionTenant();
        String adminEmail = uniqueEmail("mr-inactive");
        IdentityTenantMembership adminMembership = persistActiveIdentity(tenantId, adminEmail).block();
        assignSystemRole(tenantId, adminMembership, SystemRoleTemplate.ADMIN);
        String token = login(tenantId, adminEmail);

        String targetEmail = uniqueEmail("mr-inactive-target");
        IdentityTenantMembership targetMembership = persistActiveIdentity(tenantId, targetEmail).block();
        Role userRole = requireSystemRole(tenantId, SystemRoleTemplate.USER);

        webTestClient.put()
                .uri(IamAdminApiPaths.MEMBERSHIPS + "/" + targetMembership.id().value())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateMembershipRequest(MembershipStatus.INACTIVE))
                .exchange()
                .expectStatus().isOk();

        webTestClient.put()
                .uri(membershipRolesPath(targetMembership.id()))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReplaceMembershipRolesRequest(List.of(userRole.id().value())))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn401WithoutJwt() {
        TenantId tenantId = provisionTenant();
        IdentityTenantMembership membership = persistActiveIdentity(tenantId, uniqueEmail("mr-nojwt")).block();

        webTestClient.get()
                .uri(membershipRolesPath(membership.id()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldReturn403WithoutMembershipUpdate() {
        TenantId tenantId = provisionTenant();
        String email = uniqueEmail("mr-forbidden");
        IdentityTenantMembership membership = persistActiveIdentity(tenantId, email).block();
        assignSystemRole(tenantId, membership, SystemRoleTemplate.USER);
        String token = login(tenantId, email);

        IdentityTenantMembership target = persistActiveIdentity(tenantId, uniqueEmail("mr-fb-target")).block();

        webTestClient.get()
                .uri(membershipRolesPath(target.id()))
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    private String membershipRolesPath(MembershipId membershipId) {
        return IamAdminApiPaths.MEMBERSHIPS + "/" + membershipId.value() + "/roles";
    }

    private Role requireSystemRole(TenantId tenantId, SystemRoleTemplate template) {
        Role role = roleRepository.findByTenantIdAndCode(tenantId, template.code()).block();
        assertThat(role).isNotNull();
        return role;
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
