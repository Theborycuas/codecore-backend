package com.codecore.access.application.admin;

import com.codecore.access.application.command.AcceptInvitationCommand;
import com.codecore.access.application.command.CreateInvitationCommand;
import com.codecore.access.application.dto.AdminInvitationView;
import com.codecore.access.application.port.out.InvitationAdminQueryRepository;
import com.codecore.access.application.port.out.InvitationQueryPort;
import com.codecore.access.application.port.out.InvitationRepository;
import com.codecore.access.application.port.out.MembershipContextAccessor;
import com.codecore.access.application.port.out.SendInvitationEmailPort;
import com.codecore.access.application.port.out.TenantContextAccessor;
import com.codecore.access.domain.exception.ActiveMembershipAlreadyExistsException;
import com.codecore.access.domain.exception.InvalidInvitationStateException;
import com.codecore.access.domain.exception.InvitationNotFoundException;
import com.codecore.access.domain.exception.InviterMembershipNotFoundException;
import com.codecore.access.domain.exception.PendingInvitationAlreadyExistsException;
import com.codecore.access.domain.exception.SystemRoleNotFoundException;
import com.codecore.access.domain.model.invitation.Invitation;
import com.codecore.access.domain.valueobject.EmailAddress;
import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.domain.valueobject.InvitationRoleCode;
import com.codecore.access.domain.valueobject.InvitationStatus;
import com.codecore.access.domain.valueobject.InvitationTokenHash;
import com.codecore.access.domain.valueobject.MembershipId;
import com.codecore.access.domain.valueobject.TenantId;
import com.codecore.iam.contract.provision.TenantAccessProvisionPort;
import com.codecore.iam.contract.reference.IamActiveMembershipByEmailPort;
import com.codecore.iam.contract.reference.IamMembershipReferencePort;
import com.codecore.iam.contract.reference.IamSystemRoleReferencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationAdministrationUseCaseImplTest {

    @Mock
    private TenantContextAccessor tenantContextAccessor;
    @Mock
    private MembershipContextAccessor membershipContextAccessor;
    @Mock
    private InvitationAdminQueryRepository invitationAdminQueryRepository;
    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private InvitationQueryPort invitationQueryPort;
    @Mock
    private IamMembershipReferencePort iamMembershipReferencePort;
    @Mock
    private IamActiveMembershipByEmailPort iamActiveMembershipByEmailPort;
    @Mock
    private IamSystemRoleReferencePort iamSystemRoleReferencePort;
    @Mock
    private TenantAccessProvisionPort tenantAccessProvisionPort;
    @Mock
    private SendInvitationEmailPort sendInvitationEmailPort;
    @Mock
    private com.codecore.audit.contract.append.AuditAppendPort auditAppendPort;
    @Mock
    private TransactionalOperator transactionalOperator;

    private InvitationAdministrationUseCaseImpl useCase;

    private final TenantId tenantId = new TenantId(UUID.randomUUID());
    private final MembershipId inviterMembershipId = MembershipId.of(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        useCase = new InvitationAdministrationUseCaseImpl(
                tenantContextAccessor,
                membershipContextAccessor,
                invitationAdminQueryRepository,
                invitationRepository,
                invitationQueryPort,
                iamMembershipReferencePort,
                iamActiveMembershipByEmailPort,
                iamSystemRoleReferencePort,
                tenantAccessProvisionPort,
                sendInvitationEmailPort,
                auditAppendPort,
                transactionalOperator,
                Duration.ofDays(7)
        );
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(auditAppendPort.append(any())).thenReturn(Mono.just(UUID.randomUUID()));
    }

    @Test
    void shouldCreateInvitationWhenValidationsPass() {
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        when(membershipContextAccessor.currentMembershipId()).thenReturn(Mono.just(inviterMembershipId));
        when(iamMembershipReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(iamActiveMembershipByEmailPort.existsActiveByEmailAndTenant(any(), any())).thenReturn(Mono.just(false));
        when(invitationQueryPort.existsPendingByEmailAndTenant(any(), any())).thenReturn(Mono.just(false));
        when(iamSystemRoleReferencePort.existsSystemRoleByCodeAndTenant(eq("USER"), any())).thenReturn(Mono.just(true));
        when(invitationRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(sendInvitationEmailPort.send(any(), any(), any(), any())).thenReturn(Mono.empty());

        CreateInvitationCommand command = new CreateInvitationCommand("invitee@example.com", "USER", null);

        StepVerifier.create(useCase.execute(command))
                .assertNext(result -> {
                    assertThat(result.rawToken()).isNotBlank();
                    AdminInvitationView view = result.invitation();
                    assertThat(view.invitedEmail()).isEqualTo("invitee@example.com");
                    assertThat(view.invitedRoleCode()).isEqualTo("USER");
                    assertThat(view.status()).isEqualTo(InvitationStatus.PENDING);
                    assertThat(view.invitedByMembershipId()).isEqualTo(inviterMembershipId);
                })
                .verifyComplete();

        verify(invitationRepository).save(any());
        verify(sendInvitationEmailPort).send(any(), any(), any(), any());
    }

    @Test
    void shouldRejectCreateWhenInviterNotActive() {
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        when(membershipContextAccessor.currentMembershipId()).thenReturn(Mono.just(inviterMembershipId));
        when(iamMembershipReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.execute(new CreateInvitationCommand("a@b.com", "USER", null)))
                .expectError(InviterMembershipNotFoundException.class)
                .verify();

        verifyNoInteractions(invitationRepository);
    }

    @Test
    void shouldRejectCreateWhenActiveMembershipExists() {
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        when(membershipContextAccessor.currentMembershipId()).thenReturn(Mono.just(inviterMembershipId));
        when(iamMembershipReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(iamActiveMembershipByEmailPort.existsActiveByEmailAndTenant(any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.execute(new CreateInvitationCommand("a@b.com", "USER", null)))
                .expectError(ActiveMembershipAlreadyExistsException.class)
                .verify();

        verifyNoInteractions(invitationRepository);
    }

    @Test
    void shouldRejectCreateWhenPendingInvitationExists() {
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        when(membershipContextAccessor.currentMembershipId()).thenReturn(Mono.just(inviterMembershipId));
        when(iamMembershipReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(iamActiveMembershipByEmailPort.existsActiveByEmailAndTenant(any(), any())).thenReturn(Mono.just(false));
        when(invitationQueryPort.existsPendingByEmailAndTenant(any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.execute(new CreateInvitationCommand("a@b.com", "USER", null)))
                .expectError(PendingInvitationAlreadyExistsException.class)
                .verify();

        verifyNoInteractions(invitationRepository);
    }

    @Test
    void shouldRejectCreateWhenSystemRoleMissing() {
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        when(membershipContextAccessor.currentMembershipId()).thenReturn(Mono.just(inviterMembershipId));
        when(iamMembershipReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(iamActiveMembershipByEmailPort.existsActiveByEmailAndTenant(any(), any())).thenReturn(Mono.just(false));
        when(invitationQueryPort.existsPendingByEmailAndTenant(any(), any())).thenReturn(Mono.just(false));
        when(iamSystemRoleReferencePort.existsSystemRoleByCodeAndTenant(any(), any())).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.execute(new CreateInvitationCommand("a@b.com", "USER", null)))
                .expectError(SystemRoleNotFoundException.class)
                .verify();

        verifyNoInteractions(invitationRepository);
    }

    @Test
    void shouldRejectOwnerRoleAtDomainBoundary() {
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        when(membershipContextAccessor.currentMembershipId()).thenReturn(Mono.just(inviterMembershipId));

        StepVerifier.create(useCase.execute(new CreateInvitationCommand("a@b.com", "OWNER", null)))
                .expectError(com.codecore.access.domain.exception.InvalidDomainValueException.class)
                .verify();
    }

    @Test
    void shouldRevokeWithoutRevalidatingIamPorts() {
        Invitation invitation = pendingInvitation(Instant.now().plus(Duration.ofDays(1)));
        when(tenantContextAccessor.currentTenantId()).thenReturn(Mono.just(tenantId));
        when(invitationQueryPort.findByIdAndTenantId(any(), any())).thenReturn(Mono.just(invitation));
        when(invitationRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.revoke(invitation.id()))
                .assertNext(view -> assertThat(view.status()).isEqualTo(InvitationStatus.REVOKED))
                .verifyComplete();

        verifyNoInteractions(iamMembershipReferencePort, iamActiveMembershipByEmailPort, iamSystemRoleReferencePort);
    }

    @Test
    void shouldAcceptInvitationAndProvisionMembership() {
        Instant now = Instant.now();
        Invitation invitation = pendingInvitation(now.plus(Duration.ofDays(1)));
        when(invitationQueryPort.findByTokenHash(any())).thenReturn(Mono.just(invitation));
        when(tenantAccessProvisionPort.provision(any())).thenReturn(
                Mono.just(new com.codecore.iam.domain.valueobject.MembershipId(UUID.randomUUID()))
        );
        when(invitationRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute(new AcceptInvitationCommand("raw-token-value-here", "ValidPass1!")))
                .assertNext(result -> {
                    assertThat(result.invitation().status()).isEqualTo(InvitationStatus.ACCEPTED);
                    assertThat(result.membershipId()).isNotNull();
                })
                .verifyComplete();

        ArgumentCaptor<TenantAccessProvisionPort.ProvisionTenantAccessCommand> captor =
                ArgumentCaptor.forClass(TenantAccessProvisionPort.ProvisionTenantAccessCommand.class);
        verify(tenantAccessProvisionPort).provision(captor.capture());
        assertThat(captor.getValue().roleCode()).isEqualTo("USER");
        assertThat(captor.getValue().email().value()).isEqualTo("invitee@example.com");
    }

    @Test
    void shouldExpireAndRejectWhenTokenExpired() {
        Instant now = Instant.now();
        Invitation invitation = pendingInvitation(now.minus(Duration.ofMinutes(1)));
        when(invitationQueryPort.findByTokenHash(any())).thenReturn(Mono.just(invitation));
        when(invitationRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute(new AcceptInvitationCommand("raw-token-value-here", "ValidPass1!")))
                .expectError(InvalidInvitationStateException.class)
                .verify();

        verify(invitationRepository).save(any());
        verify(tenantAccessProvisionPort, never()).provision(any());
        assertThat(invitation.status()).isEqualTo(InvitationStatus.EXPIRED);
    }

    @Test
    void shouldFailAcceptWhenTokenUnknown() {
        when(invitationQueryPort.findByTokenHash(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(new AcceptInvitationCommand("unknown-token", null)))
                .expectError(InvitationNotFoundException.class)
                .verify();
    }

    private Invitation pendingInvitation(Instant expiresAt) {
        Instant created = Instant.now().minus(Duration.ofHours(1));
        return Invitation.create(
                InvitationId.generate(),
                tenantId,
                EmailAddress.of("invitee@example.com"),
                InvitationRoleCode.of("USER"),
                inviterMembershipId,
                InvitationTokenHash.ofHashedValue("abc123hashed"),
                expiresAt,
                created
        );
    }
}
