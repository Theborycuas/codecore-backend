package com.codecore.iam.application.authorization;

import com.codecore.iam.application.ReactorAuthorizationContextAccessor;
import com.codecore.iam.application.command.CreateTenantCommand;
import com.codecore.iam.application.dto.AccessTokenClaims;
import com.codecore.iam.application.dto.AuthenticatedPrincipal;
import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.dto.AuthenticationResponse;
import com.codecore.iam.application.port.in.AuthorizationService;
import com.codecore.iam.application.port.in.CreateTenantUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.application.port.out.TokenProvider;
import com.codecore.iam.domain.exception.IdentityNotMemberOfTenantException;
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
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.interfaces.http.dto.LoginRequest;
import com.codecore.iam.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.iam.testsupport.IamAuthorizationVerificationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FASE 14.9 — end-to-end authorization verification (not a smoke test).
 */
@SpringBootTest(
        classes = IamAuthorizationVerificationTestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=codecore-test-jwt-secret-key-minimum-32-characters-long!!",
        "security.jwt.issuer=codecore-test",
        "security.jwt.expiration=900s"
})
class AuthorizationFoundationVerificationIT extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "ValidPass1!";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CreateTenantUseCase createTenantUseCase;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ReactorAuthorizationContextAccessor authorizationContextAccessor;

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
    private TokenProvider tokenProvider;

    @Autowired
    private DatabaseClient databaseClient;

    @Test
    void verification1_ownerHasAssignedPermissions() {
        TenantId tenantId = provisionTenant();
        IdentityTenantMembership membership = persistIdentityWithMembership(tenantId, uniqueEmail("owner")).block();
        assignSystemRole(tenantId, membership, SystemRoleTemplate.OWNER);

        AuthorizationContext context = contextFor(membership);

        StepVerifier.create(authorizationService.hasPermission(context, IamPermissionCatalog.TENANT_UPDATE))
                .expectNext(true)
                .verifyComplete();
        StepVerifier.create(authorizationService.hasPermission(context, IamPermissionCatalog.USER_CREATE))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void verification2_readOnlyCannotCreateUser() {
        TenantId tenantId = provisionTenant();
        IdentityTenantMembership membership = persistIdentityWithMembership(tenantId, uniqueEmail("readonly")).block();
        assignSystemRole(tenantId, membership, SystemRoleTemplate.READ_ONLY);

        AuthorizationContext context = contextFor(membership);

        StepVerifier.create(authorizationService.hasPermission(context, IamPermissionCatalog.USER_CREATE))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void verification3_tenantIsolationDeniesCrossTenantContext() {
        TenantId tenantA = provisionTenant();
        TenantId tenantB = provisionTenant();
        IdentityTenantMembership membershipA = persistIdentityWithMembership(tenantA, uniqueEmail("iso")).block();
        assignSystemRole(tenantA, membershipA, SystemRoleTemplate.ADMIN);

        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                membershipA.identityId(),
                "iso@codecore.local",
                IdentityStatus.ACTIVE,
                Optional.of(tenantB)
        );

        StepVerifier.create(authorizationContextAccessor.resolveForPrincipal(principal))
                .expectError(IdentityNotMemberOfTenantException.class)
                .verify();
    }

    @Test
    void verification4_inactiveMembershipReturns403OnHttp() {
        TenantId tenantId = provisionTenant();
        String email = uniqueEmail("inactive");
        IdentityTenantMembership membership = persistIdentityWithMembership(tenantId, email).block();
        assignSystemRole(tenantId, membership, SystemRoleTemplate.OWNER);

        membership.deactivate();
        membershipRepository.save(membership).block();

        String token = tokenFor(membership.identityId(), email, tenantId);

        webTestClient.get()
                .uri("/api/v1/auth/user-create-probe")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void verification5_inactiveRoleDeniesPermission() {
        TenantId tenantId = provisionTenant();
        IdentityTenantMembership membership = persistIdentityWithMembership(tenantId, uniqueEmail("role-off")).block();
        Role ownerRole = assignSystemRole(tenantId, membership, SystemRoleTemplate.OWNER);

        deactivateRole(ownerRole.id().value());

        AuthorizationContext context = contextFor(membership);

        StepVerifier.create(authorizationService.hasPermission(context, IamPermissionCatalog.USER_CREATE))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void verification6_adminWithoutTenantUpdateIsDenied() {
        TenantId tenantId = provisionTenant();
        IdentityTenantMembership membership = persistIdentityWithMembership(tenantId, uniqueEmail("admin")).block();
        assignSystemRole(tenantId, membership, SystemRoleTemplate.ADMIN);

        AuthorizationContext context = contextFor(membership);

        StepVerifier.create(authorizationService.hasPermission(context, IamPermissionCatalog.TENANT_UPDATE))
                .expectNext(false)
                .verifyComplete();
        StepVerifier.create(authorizationService.hasPermission(context, IamPermissionCatalog.USER_CREATE))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void verification7_httpReturns200WithPermissionAnd403Without() {
        TenantId tenantId = provisionTenant();
        String ownerEmail = uniqueEmail("http-owner");
        IdentityTenantMembership ownerMembership = persistIdentityWithMembership(tenantId, ownerEmail).block();
        assignSystemRole(tenantId, ownerMembership, SystemRoleTemplate.OWNER);
        String ownerToken = login(tenantId, ownerEmail);

        webTestClient.get()
                .uri("/api/v1/auth/user-create-probe")
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isOk();

        String readerEmail = uniqueEmail("http-reader");
        IdentityTenantMembership readerMembership = persistIdentityWithMembership(tenantId, readerEmail).block();
        assignSystemRole(tenantId, readerMembership, SystemRoleTemplate.READ_ONLY);
        String readerToken = login(tenantId, readerEmail);

        webTestClient.get()
                .uri("/api/v1/auth/user-create-probe")
                .header("Authorization", "Bearer " + readerToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void verification8_fullJwtTenantMembershipAuthorizationChain() {
        TenantId tenantId = provisionTenant();
        String email = uniqueEmail("chain");
        IdentityTenantMembership membership = persistIdentityWithMembership(tenantId, email).block();
        assignSystemRole(tenantId, membership, SystemRoleTemplate.MANAGER);
        String token = login(tenantId, email);

        webTestClient.get()
                .uri("/api/v1/auth/user-create-probe")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden();

        deactivateRole(roleId(tenantId, SystemRoleTemplate.MANAGER));
        assignSystemRole(tenantId, membership, SystemRoleTemplate.OWNER);
        String ownerToken = login(tenantId, email);

        webTestClient.get()
                .uri("/api/v1/auth/user-create-probe")
                .header("Authorization", "Bearer " + ownerToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void verification9_denyByDefaultWithoutMembershipRoleOrPermission() {
        AuthorizationContext unknownMembership = new AuthorizationContext(
                IdentityId.generate(),
                TenantId.generate(),
                MembershipId.generate()
        );

        StepVerifier.create(authorizationService.hasPermission(unknownMembership, IamPermissionCatalog.USER_READ))
                .expectNext(false)
                .verifyComplete();

        TenantId tenantId = provisionTenant();
        IdentityTenantMembership membership = persistIdentityWithMembership(tenantId, uniqueEmail("norole")).block();
        AuthorizationContext noRoleContext = contextFor(membership);

        StepVerifier.create(authorizationService.hasPermission(noRoleContext, IamPermissionCatalog.USER_READ))
                .expectNext(false)
                .verifyComplete();
    }

    private TenantId provisionTenant() {
        String name = "Tenant-%s".formatted(UUID.randomUUID());
        return createTenantUseCase.execute(new CreateTenantCommand(name))
                .map(response -> {
                    assertThat(response.tenantId()).isNotNull();
                    return response.tenantId();
                })
                .block();
    }

    private Role assignSystemRole(
            TenantId tenantId,
            IdentityTenantMembership membership,
            SystemRoleTemplate template
    ) {
        Role role = roleRepository.findByTenantIdAndCode(tenantId, template.code()).block();
        assertThat(role).isNotNull();
        assertThat(role.systemRole()).isTrue();
        StepVerifier.create(membershipRoleRepository.assign(
                membership.id(),
                MembershipRoleAssignment.assign(role.id(), Instant.now())
        )).verifyComplete();
        return role;
    }

    private UUID roleId(TenantId tenantId, SystemRoleTemplate template) {
        return roleRepository.findByTenantIdAndCode(tenantId, template.code())
                .map(role -> role.id().value())
                .block();
    }

    private void deactivateRole(UUID roleId) {
        StepVerifier.create(databaseClient.sql("""
                        UPDATE iam.role SET status = 'INACTIVE', updated_at = NOW()
                        WHERE role_id = :roleId
                        """)
                .bind("roleId", roleId)
                .fetch()
                .rowsUpdated())
                .expectNext(1L)
                .verifyComplete();
    }

    private AuthorizationContext contextFor(IdentityTenantMembership membership) {
        return new AuthorizationContext(
                membership.identityId(),
                membership.tenantId(),
                membership.id()
        );
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

    private String tokenFor(IdentityId identityId, String email, TenantId tenantId) {
        return tokenProvider.generateAccessToken(new AccessTokenClaims(
                identityId.asString(),
                email,
                IdentityStatus.ACTIVE.name(),
                tenantId.asString()
        )).accessToken();
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
                .flatMap(saved -> membershipRepository.save(
                        IdentityTenantMembership.create(saved.id(), saved.tenantId(), Instant.now())
                ));
    }

    private static String uniqueEmail(String prefix) {
        return "%s.%s@codecore.local".formatted(prefix, UUID.randomUUID().toString().substring(0, 8));
    }
}
