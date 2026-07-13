package com.codecore.audit.interfaces.http.admin;

import com.codecore.access.interfaces.http.admin.InvitationAdminApiPaths;
import com.codecore.access.interfaces.http.admin.dto.CreateInvitationRequest;
import com.codecore.access.interfaces.http.publicapi.InvitationAcceptApiPaths;
import com.codecore.access.interfaces.http.publicapi.dto.AcceptInvitationRequest;
import com.codecore.audit.configuration.AuditOpenApiConfiguration;
import com.codecore.audit.contract.append.AuditAppendPort;
import com.codecore.audit.domain.exception.ActorMembershipNotFoundException;
import com.codecore.audit.testsupport.AbstractAuditHttpIntegrationTest;
import com.codecore.audit.testsupport.AuditAdministrationVerificationTestConfiguration;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FASE 24.7 — end-to-end Audit verification (cierre slice AuditEntry).
 */
@SpringBootTest(
        classes = AuditAdministrationVerificationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class AuditVerificationIT extends AbstractAuditHttpIntegrationTest {

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
    private AuditAppendPort auditAppendPort;

    @Test
    void verification1_appendViaPortAndGet() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        UUID resourceId = UUID.randomUUID();

        UUID entryId = auditAppendPort.append(new AuditAppendPort.AppendAuditCommand(
                tenantId.value(),
                "manual.test",
                null,
                "test",
                resourceId,
                null,
                Instant.now()
        )).block();
        assertThat(entryId).isNotNull();

        webTestClient.get()
                .uri(AuditAdminApiPaths.ENTRIES + "/" + entryId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(entryId.toString())
                .jsonPath("$.actionCode").isEqualTo("manual.test")
                .jsonPath("$.resourceType").isEqualTo("test")
                .jsonPath("$.outcome").isEqualTo("SUCCESS");
    }

    @Test
    void verification2_listFilters() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        UUID resourceA = UUID.randomUUID();
        UUID resourceB = UUID.randomUUID();

        auditAppendPort.append(new AuditAppendPort.AppendAuditCommand(
                tenantId.value(), "filter.alpha", null, "alpha", resourceA, null, Instant.now()
        )).block();
        auditAppendPort.append(new AuditAppendPort.AppendAuditCommand(
                tenantId.value(), "filter.beta", null, "beta", resourceB, null, Instant.now()
        )).block();

        webTestClient.get()
                .uri(AuditAdminApiPaths.ENTRIES + "?actionCode=filter.alpha")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].actionCode").isEqualTo("filter.alpha");

        webTestClient.get()
                .uri(AuditAdminApiPaths.ENTRIES + "?resourceType=beta&resourceId=" + resourceB)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].resourceType").isEqualTo("beta");
    }

    @Test
    void verification3_rbacAuditReadAndUnauthenticated401() {
        TenantId tenantId = provisionTenant();
        String readOnlyToken = loginAs(tenantId, SystemRoleTemplate.READ_ONLY);

        webTestClient.get()
                .uri(AuditAdminApiPaths.ENTRIES)
                .header("Authorization", "Bearer " + readOnlyToken)
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri(AuditAdminApiPaths.ENTRIES)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void verification4_crossTenantIsolation() {
        TenantId tenantA = provisionTenant();
        TenantId tenantB = provisionTenant();
        String adminA = loginAs(tenantA, SystemRoleTemplate.ADMIN);
        String adminB = loginAs(tenantB, SystemRoleTemplate.ADMIN);

        UUID entryInB = auditAppendPort.append(new AuditAppendPort.AppendAuditCommand(
                tenantB.value(), "cross.tenant", null, "test", UUID.randomUUID(), null, Instant.now()
        )).block();

        webTestClient.get()
                .uri(AuditAdminApiPaths.ENTRIES + "/" + entryInB)
                .header("Authorization", "Bearer " + adminA)
                .exchange()
                .expectStatus().isNotFound();

        webTestClient.get()
                .uri(AuditAdminApiPaths.ENTRIES + "/" + entryInB)
                .header("Authorization", "Bearer " + adminB)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void verification5_creatingInvitationProducesAuditInvitationCreated() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        Map<String, String> created = createInvitation(adminToken, uniqueEmail("invite-create"), "USER");

        webTestClient.get()
                .uri(AuditAdminApiPaths.ENTRIES + "?actionCode=invitation.created&resourceId=" + created.get("id"))
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].resourceType").isEqualTo("invitation")
                .jsonPath("$.content[0].actionCode").isEqualTo("invitation.created");
    }

    @Test
    void verification6_acceptInvitationProducesAuditInvitationAccepted() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        Map<String, String> created = createInvitation(adminToken, uniqueEmail("invite-accept"), "USER");

        webTestClient.post()
                .uri(InvitationAcceptApiPaths.ACCEPT)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AcceptInvitationRequest(created.get("token"), PASSWORD))
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri(AuditAdminApiPaths.ENTRIES + "?actionCode=invitation.accepted&resourceId=" + created.get("id"))
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].actionCode").isEqualTo("invitation.accepted");
    }

    @Test
    void verification7_appendWithInvalidActorMembershipFails() {
        TenantId tenantId = provisionTenant();

        StepVerifier.create(auditAppendPort.append(new AuditAppendPort.AppendAuditCommand(
                        tenantId.value(),
                        "invalid.actor",
                        UUID.randomUUID(),
                        "test",
                        UUID.randomUUID(),
                        null,
                        Instant.now()
                )))
                .expectError(ActorMembershipNotFoundException.class)
                .verify();
    }

    @Test
    void verification8_noHttpPostToCreateAudit() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);

        webTestClient.post()
                .uri(AuditAdminApiPaths.ENTRIES)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().value(status -> assertThat(status).isIn(404, 405));

        webTestClient.get()
                .uri("/v3/api-docs/" + AuditOpenApiConfiguration.AUDIT_ADMINISTRATION_GROUP)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['" + AuditAdminApiPaths.ENTRIES + "']").exists();
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
