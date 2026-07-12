package com.codecore.patient.interfaces.http.admin;

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
import com.codecore.organization.interfaces.http.admin.OrgAdminApiPaths;
import com.codecore.organization.interfaces.http.admin.dto.CreateOrganizationRequest;
import com.codecore.patient.configuration.PatientOpenApiConfiguration;
import com.codecore.patient.interfaces.http.admin.dto.CreatePatientRequest;
import com.codecore.patient.interfaces.http.admin.dto.ExternalIdentifierRequest;
import com.codecore.patient.interfaces.http.admin.dto.UpdatePatientRequest;
import com.codecore.patient.testsupport.AbstractPatientHttpIntegrationTest;
import com.codecore.patient.testsupport.PatientAdministrationVerificationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FASE 17.7 — end-to-end Patient Clinical Foundation verification (cierre BC).
 */
@SpringBootTest(
        classes = PatientAdministrationVerificationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class PatientVerificationIT extends AbstractPatientHttpIntegrationTest {

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
    void verification1_fullPatientRegistryJourney() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String orgId = createOrganization(adminToken, "CLINIC_CORE", "Clinic Core");

        String patientWithOrg = createPatient(
                adminToken,
                "María García",
                UUID.fromString(orgId),
                List.of(new ExternalIdentifierRequest("MRN", "P-001"))
        );
        createPatient(adminToken, "Buddy", null, List.of());
        createPatient(adminToken, "Ana López", null, List.of());

        webTestClient.get()
                .uri(PatientAdminApiPaths.PATIENTS + "?page=0&size=20")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(3)
                .jsonPath("$.content[0].tenantId").doesNotExist();

        webTestClient.get()
                .uri(PatientAdminApiPaths.PATIENTS + "?primaryOrganizationId=" + orgId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].id").isEqualTo(patientWithOrg);

        webTestClient.put()
                .uri(PatientAdminApiPaths.PATIENTS + "/" + patientWithOrg)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdatePatientRequest(
                        "María G.",
                        "maria@example.com",
                        null,
                        null,
                        UUID.fromString(orgId),
                        List.of(new ExternalIdentifierRequest("MRN", "P-001"))
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.displayName").isEqualTo("María G.");

        webTestClient.post()
                .uri(PatientAdminApiPaths.PATIENTS + "/" + patientWithOrg + "/archive")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ARCHIVED");

        webTestClient.get()
                .uri(PatientAdminApiPaths.PATIENTS + "?status=ACTIVE")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2);

        webTestClient.get()
                .uri(PatientAdminApiPaths.PATIENTS + "/" + patientWithOrg)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ARCHIVED");

        webTestClient.post()
                .uri(PatientAdminApiPaths.PATIENTS + "/" + patientWithOrg + "/activate")
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
                .uri(PatientAdminApiPaths.PATIENTS)
                .header("Authorization", "Bearer " + readerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreatePatientRequest("Denied", null, null, null, null, List.of()))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void verification3_crossTenantAccessReturns404() {
        TenantId tenantA = provisionTenant();
        TenantId tenantB = provisionTenant();
        String adminA = loginAs(tenantA, SystemRoleTemplate.ADMIN);
        String adminB = loginAs(tenantB, SystemRoleTemplate.ADMIN);
        String patientInB = createPatient(adminB, "Tenant B Patient", null, List.of());

        webTestClient.get()
                .uri(PatientAdminApiPaths.PATIENTS + "/" + patientInB)
                .header("Authorization", "Bearer " + adminA)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void verification4_invalidPrimaryOrganizationReturns404() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);

        webTestClient.post()
                .uri(PatientAdminApiPaths.PATIENTS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreatePatientRequest(
                        "No Org",
                        null,
                        null,
                        null,
                        UUID.randomUUID(),
                        List.of()
                ))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void verification5_managerCanArchivePatientPerMatrix() {
        // Unlike organization:archive (MANAGER denied), patient:archive is granted to MANAGER (PASO 17.5).
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String managerToken = loginAs(tenantId, SystemRoleTemplate.MANAGER);
        String patientId = createPatient(adminToken, "Manager Archive", null, List.of());

        webTestClient.get()
                .uri(PatientAdminApiPaths.PATIENTS + "/" + patientId)
                .header("Authorization", "Bearer " + managerToken)
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri(PatientAdminApiPaths.PATIENTS + "/" + patientId + "/archive")
                .header("Authorization", "Bearer " + managerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ARCHIVED");
    }

    @Test
    void verification6_duplicateExternalIdentifierReturns409() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        List<ExternalIdentifierRequest> identifiers = List.of(new ExternalIdentifierRequest("MRN", "DUP-1"));

        webTestClient.post()
                .uri(PatientAdminApiPaths.PATIENTS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreatePatientRequest("First", null, null, null, null, identifiers))
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri(PatientAdminApiPaths.PATIENTS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreatePatientRequest("Second", null, null, null, null, identifiers))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void verification7_openApiDocumentsClinicalAdministrationSurface() {
        webTestClient.get()
                .uri("/v3/api-docs/" + PatientOpenApiConfiguration.CLINICAL_ADMINISTRATION_GROUP)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['" + PatientAdminApiPaths.PATIENTS + "']").exists();
    }

    @Test
    void verification8_unauthenticatedRequestsReturn401() {
        webTestClient.get()
                .uri(PatientAdminApiPaths.PATIENTS)
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.get()
                .uri(PatientAdminApiPaths.PATIENTS + "/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private String createPatient(
            String token,
            String displayName,
            UUID primaryOrganizationId,
            List<ExternalIdentifierRequest> identifiers
    ) {
        AtomicReference<String> idRef = new AtomicReference<>();
        webTestClient.post()
                .uri(PatientAdminApiPaths.PATIENTS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreatePatientRequest(
                        displayName,
                        null,
                        null,
                        null,
                        primaryOrganizationId,
                        identifiers
                ))
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
