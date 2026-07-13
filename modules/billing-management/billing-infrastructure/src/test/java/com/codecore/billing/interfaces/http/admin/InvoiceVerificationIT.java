package com.codecore.billing.interfaces.http.admin;

import com.codecore.billing.configuration.BillingOpenApiConfiguration;
import com.codecore.billing.interfaces.http.admin.dto.CreateInvoiceRequest;
import com.codecore.billing.interfaces.http.admin.dto.InvoiceLineRequest;
import com.codecore.billing.interfaces.http.admin.dto.UpdateInvoiceRequest;
import com.codecore.billing.testsupport.AbstractInvoiceHttpIntegrationTest;
import com.codecore.billing.testsupport.InvoiceAdministrationVerificationTestConfiguration;
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
import com.codecore.inventory.interfaces.http.admin.ItemAdminApiPaths;
import com.codecore.inventory.interfaces.http.admin.dto.CreateItemRequest;
import com.codecore.organization.interfaces.http.admin.OrgAdminApiPaths;
import com.codecore.organization.interfaces.http.admin.dto.CreateOrganizationRequest;
import com.codecore.patient.interfaces.http.admin.PatientAdminApiPaths;
import com.codecore.patient.interfaces.http.admin.dto.CreatePatientRequest;
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
 * FASE 21.7 — end-to-end Invoice Billing verification (cierre slice Invoice).
 */
@SpringBootTest(
        classes = InvoiceAdministrationVerificationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class InvoiceVerificationIT extends AbstractInvoiceHttpIntegrationTest {

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
    void verification1_fullInvoiceLifecycleJourney() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String orgId = createOrganization(adminToken, "BILL_ISSUER", "Billing Issuer Org");
        String patientId = createPatient(adminToken, "Jane Billing");
        String itemId = createItem(adminToken, "Consultation Fee", "SKU-CONSULT");

        String invoiceId = createInvoice(
                adminToken,
                UUID.fromString(orgId),
                UUID.fromString(patientId),
                null,
                "INV-0001",
                "USD",
                List.of(new InvoiceLineRequest("Consultation", 15000, UUID.fromString(itemId), null))
        );

        webTestClient.get()
                .uri(InvoiceAdminApiPaths.INVOICES + "?page=0&size=20")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].id").isEqualTo(invoiceId)
                .jsonPath("$.content[0].status").isEqualTo("DRAFT");

        webTestClient.get()
                .uri(InvoiceAdminApiPaths.INVOICES + "/" + invoiceId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalAmountMinor").isEqualTo(15000)
                .jsonPath("$.currency").isEqualTo("USD")
                .jsonPath("$.lines.length()").isEqualTo(1);

        webTestClient.put()
                .uri(InvoiceAdminApiPaths.INVOICES + "/" + invoiceId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateInvoiceRequest(
                        UUID.fromString(orgId),
                        UUID.fromString(patientId),
                        null,
                        "INV-0001",
                        "USD",
                        List.of(
                                new InvoiceLineRequest("Consultation", 15000, UUID.fromString(itemId), null),
                                new InvoiceLineRequest("Follow-up", 5000, null, null)
                        )
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalAmountMinor").isEqualTo(20000)
                .jsonPath("$.lines.length()").isEqualTo(2);

        webTestClient.post()
                .uri(InvoiceAdminApiPaths.INVOICES + "/" + invoiceId + "/issue")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ISSUED");

        webTestClient.get()
                .uri(InvoiceAdminApiPaths.INVOICES + "?status=DRAFT")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(0);

        webTestClient.get()
                .uri(InvoiceAdminApiPaths.INVOICES + "?status=ISSUED")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].id").isEqualTo(invoiceId);

        webTestClient.post()
                .uri(InvoiceAdminApiPaths.INVOICES + "/" + invoiceId + "/void")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("VOIDED");

        webTestClient.get()
                .uri(InvoiceAdminApiPaths.INVOICES + "?status=VOIDED")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].id").isEqualTo(invoiceId);
    }

    @Test
    void verification2_readOnlyCannotCreateInvoice() {
        TenantId tenantId = provisionTenant();
        String readerToken = loginAs(tenantId, SystemRoleTemplate.READ_ONLY);

        webTestClient.post()
                .uri(InvoiceAdminApiPaths.INVOICES)
                .header("Authorization", "Bearer " + readerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateInvoiceRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        "USD",
                        List.of(new InvoiceLineRequest("Denied", 100, null, null))
                ))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void verification3_crossTenantAccessReturns404() {
        TenantId tenantA = provisionTenant();
        TenantId tenantB = provisionTenant();
        String adminA = loginAs(tenantA, SystemRoleTemplate.ADMIN);
        String adminB = loginAs(tenantB, SystemRoleTemplate.ADMIN);

        String orgB = createOrganization(adminB, "TENANT_B_ORG", "Tenant B Org");
        String patientB = createPatient(adminB, "Tenant B Patient");
        String invoiceInB = createInvoice(
                adminB,
                UUID.fromString(orgB),
                UUID.fromString(patientB),
                null,
                null,
                "USD",
                List.of(new InvoiceLineRequest("Tenant B line", 100, null, null))
        );

        webTestClient.get()
                .uri(InvoiceAdminApiPaths.INVOICES + "/" + invoiceInB)
                .header("Authorization", "Bearer " + adminA)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void verification4_invalidIssuerOrBillToReferencesReturn404() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String orgId = createOrganization(token, "REF_ORG", "Reference Org");
        String patientId = createPatient(token, "Reference Patient");

        webTestClient.post()
                .uri(InvoiceAdminApiPaths.INVOICES)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateInvoiceRequest(
                        UUID.randomUUID(),
                        UUID.fromString(patientId),
                        null,
                        null,
                        "USD",
                        List.of(new InvoiceLineRequest("Bad issuer", 100, null, null))
                ))
                .exchange()
                .expectStatus().isNotFound();

        webTestClient.post()
                .uri(InvoiceAdminApiPaths.INVOICES)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateInvoiceRequest(
                        UUID.fromString(orgId),
                        UUID.randomUUID(),
                        null,
                        null,
                        "USD",
                        List.of(new InvoiceLineRequest("Bad patient", 100, null, null))
                ))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void verification5_managerCanIssueAndVoidPerMatrix() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String managerToken = loginAs(tenantId, SystemRoleTemplate.MANAGER);
        String orgId = createOrganization(adminToken, "MGR_ORG", "Manager Org");
        String patientId = createPatient(adminToken, "Manager Patient");

        String invoiceId = createInvoice(
                adminToken,
                UUID.fromString(orgId),
                UUID.fromString(patientId),
                null,
                null,
                "USD",
                List.of(new InvoiceLineRequest("Manager line", 4200, null, null))
        );

        webTestClient.post()
                .uri(InvoiceAdminApiPaths.INVOICES + "/" + invoiceId + "/issue")
                .header("Authorization", "Bearer " + managerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ISSUED");

        webTestClient.post()
                .uri(InvoiceAdminApiPaths.INVOICES + "/" + invoiceId + "/void")
                .header("Authorization", "Bearer " + managerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("VOIDED");
    }

    @Test
    void verification6_duplicateInvoiceNumberReturns409() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String orgId = createOrganization(token, "DUP_ORG", "Duplicate Org");
        String patientId = createPatient(token, "Duplicate Patient");

        createInvoice(
                token,
                UUID.fromString(orgId),
                UUID.fromString(patientId),
                null,
                "DUP-0001",
                "USD",
                List.of(new InvoiceLineRequest("First", 100, null, null))
        );

        webTestClient.post()
                .uri(InvoiceAdminApiPaths.INVOICES)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateInvoiceRequest(
                        UUID.fromString(orgId),
                        UUID.fromString(patientId),
                        null,
                        "DUP-0001",
                        "USD",
                        List.of(new InvoiceLineRequest("Second", 200, null, null))
                ))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void verification7_openApiDocumentsBillingAdministrationSurface() {
        webTestClient.get()
                .uri("/v3/api-docs/" + BillingOpenApiConfiguration.BILLING_ADMINISTRATION_GROUP)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['" + InvoiceAdminApiPaths.INVOICES + "']").exists();
    }

    @Test
    void verification8_unauthenticatedRequestsReturn401() {
        webTestClient.get()
                .uri(InvoiceAdminApiPaths.INVOICES)
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.get()
                .uri(InvoiceAdminApiPaths.INVOICES + "/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private String createInvoice(
            String token,
            UUID issuerOrganizationId,
            UUID billToPatientId,
            UUID billToOrganizationId,
            String invoiceNumber,
            String currency,
            List<InvoiceLineRequest> lines
    ) {
        AtomicReference<String> idRef = new AtomicReference<>();
        webTestClient.post()
                .uri(InvoiceAdminApiPaths.INVOICES)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateInvoiceRequest(
                        issuerOrganizationId,
                        billToPatientId,
                        billToOrganizationId,
                        invoiceNumber,
                        currency,
                        lines
                ))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").value(id -> idRef.set(id.toString()));
        assertThat(idRef.get()).isNotNull();
        return idRef.get();
    }

    private String createItem(String token, String displayName, String code) {
        AtomicReference<String> idRef = new AtomicReference<>();
        webTestClient.post()
                .uri(ItemAdminApiPaths.ITEMS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateItemRequest(displayName, code, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").value(id -> idRef.set(id.toString()));
        assertThat(idRef.get()).isNotNull();
        return idRef.get();
    }

    private String createPatient(String token, String displayName) {
        AtomicReference<String> idRef = new AtomicReference<>();
        webTestClient.post()
                .uri(PatientAdminApiPaths.PATIENTS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreatePatientRequest(displayName, null, null, null, null, null))
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
