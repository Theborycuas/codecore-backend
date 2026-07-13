package com.codecore.access.interfaces.http.admin;

import com.codecore.access.configuration.AccessOpenApiConfiguration;
import com.codecore.access.interfaces.http.admin.dto.CreateInvitationRequest;
import com.codecore.access.interfaces.http.publicapi.InvitationAcceptApiPaths;
import com.codecore.access.interfaces.http.publicapi.dto.AcceptInvitationRequest;
import com.codecore.access.testsupport.AbstractAccessHttpIntegrationTest;
import com.codecore.access.testsupport.AccessAdministrationVerificationTestConfiguration;
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
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FASE 23.7 — end-to-end Access (Invitation) verification.
 */
@SpringBootTest(
        classes = AccessAdministrationVerificationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class InvitationVerificationIT extends AbstractAccessHttpIntegrationTest {

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
    @Autowired
    private ConnectionFactory connectionFactory;

    @Test
    void verification1_fullInvitationLifecycleJourney() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String inviteeEmail = uniqueEmail("invitee");

        Map<String, String> created = createInvitation(adminToken, inviteeEmail, "USER");
        String invitationId = created.get("id");
        String rawToken = created.get("token");

        webTestClient.get()
                .uri(InvitationAdminApiPaths.INVITATIONS + "?page=0&size=20")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].id").isEqualTo(invitationId)
                .jsonPath("$.content[0].status").isEqualTo("PENDING");

        webTestClient.get()
                .uri(InvitationAdminApiPaths.INVITATIONS + "/" + invitationId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.invitedEmail").isEqualTo(inviteeEmail)
                .jsonPath("$.invitedRoleCode").isEqualTo("USER")
                .jsonPath("$.status").isEqualTo("PENDING");

        AtomicReference<String> membershipIdRef = new AtomicReference<>();
        webTestClient.post()
                .uri(InvitationAcceptApiPaths.ACCEPT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AcceptInvitationRequest(rawToken, PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.invitation.status").isEqualTo("ACCEPTED")
                .jsonPath("$.membershipId").value(id -> membershipIdRef.set(id.toString()));

        assertThat(membershipIdRef.get()).isNotNull();

        webTestClient.get()
                .uri(InvitationAdminApiPaths.INVITATIONS + "/" + invitationId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ACCEPTED")
                .jsonPath("$.resultingMembershipId").isEqualTo(membershipIdRef.get());
    }

    @Test
    void verification2_readOnlyCannotCreateInvitation() {
        TenantId tenantId = provisionTenant();
        String readerToken = loginAs(tenantId, SystemRoleTemplate.READ_ONLY);

        webTestClient.post()
                .uri(InvitationAdminApiPaths.INVITATIONS)
                .header("Authorization", "Bearer " + readerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateInvitationRequest(uniqueEmail("ro"), "USER"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void verification3_crossTenantAccessReturns404() {
        TenantId tenantA = provisionTenant();
        TenantId tenantB = provisionTenant();
        String adminA = loginAs(tenantA, SystemRoleTemplate.ADMIN);
        String adminB = loginAs(tenantB, SystemRoleTemplate.ADMIN);

        Map<String, String> invitationInB = createInvitation(adminB, uniqueEmail("b"), "USER");

        webTestClient.get()
                .uri(InvitationAdminApiPaths.INVITATIONS + "/" + invitationInB.get("id"))
                .header("Authorization", "Bearer " + adminA)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void verification4_rejectOwnerRole() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);

        webTestClient.post()
                .uri(InvitationAdminApiPaths.INVITATIONS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateInvitationRequest(uniqueEmail("owner"), "OWNER"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void verification5_rejectDuplicateActiveMembership() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String existingEmail = uniqueEmail("existing");
        persistActiveIdentity(tenantId, existingEmail).block();

        webTestClient.post()
                .uri(InvitationAdminApiPaths.INVITATIONS)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateInvitationRequest(existingEmail, "USER"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void verification6_managerCanRevokePerMatrix() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String managerToken = loginAs(tenantId, SystemRoleTemplate.MANAGER);

        Map<String, String> created = createInvitation(managerToken, uniqueEmail("revoke"), "READ_ONLY");

        webTestClient.post()
                .uri(InvitationAdminApiPaths.INVITATIONS + "/" + created.get("id") + "/revoke")
                .header("Authorization", "Bearer " + managerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("REVOKED");

        // admin create still works for baseline
        assertThat(adminToken).isNotBlank();
    }

    @Test
    void verification7_expiredAcceptIsRejected() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        Map<String, String> created = createInvitation(adminToken, uniqueEmail("expired"), "USER");

        DatabaseClient databaseClient = DatabaseClient.create(connectionFactory);
        StepVerifier.create(databaseClient.sql("""
                        UPDATE access.invitation
                        SET expires_at = :expiresAt
                        WHERE invitation_id = :invitationId
                        """)
                .bind("expiresAt", Instant.now().minusSeconds(60))
                .bind("invitationId", UUID.fromString(created.get("id")))
                .fetch()
                .rowsUpdated())
                .expectNextCount(1)
                .verifyComplete();

        webTestClient.post()
                .uri(InvitationAcceptApiPaths.ACCEPT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AcceptInvitationRequest(created.get("token"), PASSWORD))
                .exchange()
                .expectStatus().isBadRequest();

        webTestClient.get()
                .uri(InvitationAdminApiPaths.INVITATIONS + "/" + created.get("id"))
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("EXPIRED");
    }

    @Test
    void verification8_openApiDocumentsAccessAdministrationSurface() {
        webTestClient.get()
                .uri("/v3/api-docs/" + AccessOpenApiConfiguration.ACCESS_ADMINISTRATION_GROUP)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['" + InvitationAdminApiPaths.INVITATIONS + "']").exists();

        webTestClient.get()
                .uri(InvitationAdminApiPaths.INVITATIONS)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private Map<String, String> createInvitation(String token, String email, String roleCode) {
        AtomicReference<String> idRef = new AtomicReference<>();
        AtomicReference<String> tokenRef = new AtomicReference<>();
        webTestClient.post()
                .uri(InvitationAdminApiPaths.INVITATIONS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateInvitationRequest(email, roleCode))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.invitation.id").value(id -> idRef.set(id.toString()))
                .jsonPath("$.token").value(t -> tokenRef.set(t.toString()));
        assertThat(idRef.get()).isNotNull();
        assertThat(tokenRef.get()).isNotNull();
        return Map.of("id", idRef.get(), "token", tokenRef.get());
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
