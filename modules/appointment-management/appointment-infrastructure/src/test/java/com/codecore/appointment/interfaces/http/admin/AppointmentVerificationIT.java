package com.codecore.appointment.interfaces.http.admin;

import com.codecore.appointment.configuration.AppointmentOpenApiConfiguration;
import com.codecore.appointment.interfaces.http.admin.dto.CreateAppointmentRequest;
import com.codecore.appointment.interfaces.http.admin.dto.UpdateAppointmentRequest;
import com.codecore.appointment.testsupport.AbstractAppointmentHttpIntegrationTest;
import com.codecore.appointment.testsupport.AppointmentAdministrationVerificationTestConfiguration;
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
import com.codecore.organization.interfaces.http.admin.dto.CreateOfficeRequest;
import com.codecore.organization.interfaces.http.admin.dto.CreateOrganizationRequest;
import com.codecore.organization.interfaces.http.admin.dto.CreateStaffAssignmentRequest;
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
 * FASE 18.7 — end-to-end Appointment Scheduling verification (cierre BC).
 */
@SpringBootTest(
        classes = AppointmentAdministrationVerificationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class AppointmentVerificationIT extends AbstractAppointmentHttpIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";
    private static final Instant START = Instant.parse("2026-07-12T14:00:00Z");
    private static final Instant END = Instant.parse("2026-07-12T15:00:00Z");

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
    void verification1_fullAppointmentSchedulingJourney() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        Fixture fixture = createSchedulingFixture(tenantId, adminToken);

        String firstId = createAppointment(adminToken, fixture, START, END);
        createAppointment(
                adminToken,
                fixture,
                Instant.parse("2026-07-13T09:00:00Z"),
                Instant.parse("2026-07-13T10:00:00Z")
        );
        createAppointment(
                adminToken,
                fixture,
                Instant.parse("2026-07-14T09:00:00Z"),
                Instant.parse("2026-07-14T10:00:00Z")
        );

        webTestClient.get()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS + "?page=0&size=20")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(3)
                .jsonPath("$.content[0].tenantId").doesNotExist();

        webTestClient.get()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS + "?organizationId=" + fixture.organizationId())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(3);

        Instant newStart = Instant.parse("2026-07-15T11:00:00Z");
        Instant newEnd = Instant.parse("2026-07-15T12:00:00Z");
        webTestClient.put()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS + "/" + firstId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateAppointmentRequest(
                        fixture.patientId(),
                        fixture.staffAssignmentId(),
                        fixture.organizationId(),
                        fixture.officeId(),
                        newStart,
                        newEnd
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.officeId").isEqualTo(fixture.officeId().toString())
                .jsonPath("$.startsAt").isEqualTo(newStart.toString());

        webTestClient.post()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS + "/" + firstId + "/cancel")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("CANCELLED");

        webTestClient.get()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS + "?status=SCHEDULED")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2);

        webTestClient.get()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS + "/" + firstId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("CANCELLED");

        String toComplete = createAppointment(
                adminToken,
                fixture,
                Instant.parse("2026-07-16T09:00:00Z"),
                Instant.parse("2026-07-16T10:00:00Z")
        );
        webTestClient.post()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS + "/" + toComplete + "/complete")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("COMPLETED");
    }

    @Test
    void verification2_rbacDeniesCreateWithoutPermission() {
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String readerToken = loginAs(tenantId, SystemRoleTemplate.READ_ONLY);
        Fixture fixture = createSchedulingFixture(tenantId, adminToken);

        webTestClient.post()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS)
                .header("Authorization", "Bearer " + readerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateAppointmentRequest(
                        fixture.patientId(),
                        fixture.staffAssignmentId(),
                        fixture.organizationId(),
                        null,
                        START,
                        END
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
        Fixture fixtureB = createSchedulingFixture(tenantB, adminB);
        String appointmentInB = createAppointment(adminB, fixtureB, START, END);

        webTestClient.get()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS + "/" + appointmentInB)
                .header("Authorization", "Bearer " + adminA)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void verification4_invalidReferencesReturn404() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        Fixture fixture = createSchedulingFixture(tenantId, token);

        webTestClient.post()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateAppointmentRequest(
                        UUID.randomUUID(),
                        fixture.staffAssignmentId(),
                        fixture.organizationId(),
                        null,
                        START,
                        END
                ))
                .exchange()
                .expectStatus().isNotFound();

        webTestClient.post()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateAppointmentRequest(
                        fixture.patientId(),
                        fixture.staffAssignmentId(),
                        UUID.randomUUID(),
                        null,
                        START,
                        END
                ))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void verification5_managerCanCancelAppointmentPerMatrix() {
        // MANAGER has appointment:cancel (PASO 18.5) — daily operations.
        TenantId tenantId = provisionTenant();
        String adminToken = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        String managerToken = loginAs(tenantId, SystemRoleTemplate.MANAGER);
        Fixture fixture = createSchedulingFixture(tenantId, adminToken);
        String appointmentId = createAppointment(adminToken, fixture, START, END);

        webTestClient.get()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS + "/" + appointmentId)
                .header("Authorization", "Bearer " + managerToken)
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS + "/" + appointmentId + "/cancel")
                .header("Authorization", "Bearer " + managerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("CANCELLED");
    }

    @Test
    void verification6_staffOrganizationCoherenceMismatchReturns409() {
        TenantId tenantId = provisionTenant();
        String token = loginAs(tenantId, SystemRoleTemplate.ADMIN);
        Fixture fixture = createSchedulingFixture(tenantId, token);
        String otherOrgId = createOrganization(token, "OTH_" + UUID.randomUUID().toString().substring(0, 6), "Other Org");

        webTestClient.post()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateAppointmentRequest(
                        fixture.patientId(),
                        fixture.staffAssignmentId(),
                        UUID.fromString(otherOrgId),
                        null,
                        START,
                        END
                ))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void verification7_openApiDocumentsSchedulingAdministrationSurface() {
        webTestClient.get()
                .uri("/v3/api-docs/" + AppointmentOpenApiConfiguration.SCHEDULING_ADMINISTRATION_GROUP)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['" + AppointmentAdminApiPaths.APPOINTMENTS + "']").exists();
    }

    @Test
    void verification8_unauthenticatedRequestsReturn401() {
        webTestClient.get()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS)
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.get()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS + "/" + UUID.randomUUID())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private Fixture createSchedulingFixture(TenantId tenantId, String token) {
        String orgId = createOrganization(token, "SCH_" + UUID.randomUUID().toString().substring(0, 6), "Scheduling Org");
        String officeId = createOffice(token, UUID.fromString(orgId), "MAIN", "Main Office");
        IdentityTenantMembership membership = persistActiveIdentity(tenantId, uniqueEmail("staff")).block();
        assertThat(membership).isNotNull();
        String staffId = createStaffAssignment(token, membership.id().value(), UUID.fromString(orgId), null);
        String patientId = createPatient(token, "Care Subject");
        return new Fixture(
                UUID.fromString(patientId),
                UUID.fromString(staffId),
                UUID.fromString(orgId),
                UUID.fromString(officeId)
        );
    }

    private String createAppointment(String token, Fixture fixture, Instant startsAt, Instant endsAt) {
        AtomicReference<String> idRef = new AtomicReference<>();
        webTestClient.post()
                .uri(AppointmentAdminApiPaths.APPOINTMENTS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateAppointmentRequest(
                        fixture.patientId(),
                        fixture.staffAssignmentId(),
                        fixture.organizationId(),
                        null,
                        startsAt,
                        endsAt
                ))
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
                .bodyValue(new CreatePatientRequest(displayName, null, null, null, null, List.of()))
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

    private String createOffice(String token, UUID organizationId, String code, String name) {
        AtomicReference<String> idRef = new AtomicReference<>();
        webTestClient.post()
                .uri(OrgAdminApiPaths.OFFICES)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateOfficeRequest(organizationId, code, name))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").value(id -> idRef.set(id.toString()));
        assertThat(idRef.get()).isNotNull();
        return idRef.get();
    }

    private String createStaffAssignment(String token, UUID membershipId, UUID organizationId, UUID officeId) {
        AtomicReference<String> idRef = new AtomicReference<>();
        webTestClient.post()
                .uri(OrgAdminApiPaths.STAFF_ASSIGNMENTS)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CreateStaffAssignmentRequest(membershipId, organizationId, officeId))
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

    private record Fixture(
            UUID patientId,
            UUID staffAssignmentId,
            UUID organizationId,
            UUID officeId
    ) {
    }
}
