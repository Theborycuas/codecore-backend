package com.codecore.access.application.admin;

import com.codecore.access.application.command.AcceptInvitationCommand;
import com.codecore.access.application.command.CreateInvitationCommand;
import com.codecore.access.application.dto.AcceptInvitationResult;
import com.codecore.access.application.dto.AdminInvitationView;
import com.codecore.access.application.dto.CreateInvitationResult;
import com.codecore.access.application.dto.PagedResult;
import com.codecore.access.application.port.in.AcceptInvitationUseCase;
import com.codecore.access.application.port.in.CreateInvitationUseCase;
import com.codecore.access.application.port.in.GetInvitationUseCase;
import com.codecore.access.application.port.in.ListInvitationsUseCase;
import com.codecore.access.application.port.in.RevokeInvitationUseCase;
import com.codecore.access.application.port.out.InvitationAdminQueryRepository;
import com.codecore.access.application.port.out.InvitationQueryPort;
import com.codecore.access.application.port.out.InvitationRepository;
import com.codecore.access.application.port.out.MembershipContextAccessor;
import com.codecore.access.application.port.out.SendInvitationEmailPort;
import com.codecore.access.application.port.out.TenantContextAccessor;
import com.codecore.access.application.query.InvitationListQuery;
import com.codecore.access.application.query.PageQuery;
import com.codecore.access.application.security.InvitationTokenHasher;
import com.codecore.access.domain.exception.ActiveMembershipAlreadyExistsException;
import com.codecore.access.domain.exception.InvalidDomainValueException;
import com.codecore.access.domain.exception.InvalidInvitationStateException;
import com.codecore.access.domain.exception.InvitationNotFoundException;
import com.codecore.access.domain.exception.InviterMembershipNotFoundException;
import com.codecore.access.domain.exception.PendingInvitationAlreadyExistsException;
import com.codecore.access.domain.exception.SystemRoleNotFoundException;
import com.codecore.access.domain.model.invitation.Invitation;
import com.codecore.access.domain.valueobject.EmailAddress;
import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.domain.valueobject.InvitationRoleCode;
import com.codecore.access.domain.valueobject.InvitationTokenHash;
import com.codecore.access.domain.valueobject.MembershipId;
import com.codecore.access.domain.valueobject.TenantId;
import com.codecore.iam.contract.provision.TenantAccessProvisionPort;
import com.codecore.iam.contract.reference.IamActiveMembershipByEmailPort;
import com.codecore.iam.contract.reference.IamMembershipReferencePort;
import com.codecore.iam.contract.reference.IamSystemRoleReferencePort;
import com.codecore.iam.domain.valueobject.RawPassword;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Invitation administration + public accept use cases (FASE 23.6) — write-time validation via
 * IAM contract ports (ADR-013 / ADR-019). {@code revoke} does not revalidate IAM ports.
 */
public final class InvitationAdministrationUseCaseImpl
        implements ListInvitationsUseCase,
        GetInvitationUseCase,
        CreateInvitationUseCase,
        RevokeInvitationUseCase,
        AcceptInvitationUseCase {

    public static final Duration DEFAULT_INVITATION_TTL = Duration.ofDays(7);

    private final TenantContextAccessor tenantContextAccessor;
    private final MembershipContextAccessor membershipContextAccessor;
    private final InvitationAdminQueryRepository invitationAdminQueryRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationQueryPort invitationQueryPort;
    private final IamMembershipReferencePort iamMembershipReferencePort;
    private final IamActiveMembershipByEmailPort iamActiveMembershipByEmailPort;
    private final IamSystemRoleReferencePort iamSystemRoleReferencePort;
    private final TenantAccessProvisionPort tenantAccessProvisionPort;
    private final SendInvitationEmailPort sendInvitationEmailPort;
    private final TransactionalOperator transactionalOperator;
    private final Duration invitationTtl;

    public InvitationAdministrationUseCaseImpl(
            TenantContextAccessor tenantContextAccessor,
            MembershipContextAccessor membershipContextAccessor,
            InvitationAdminQueryRepository invitationAdminQueryRepository,
            InvitationRepository invitationRepository,
            InvitationQueryPort invitationQueryPort,
            IamMembershipReferencePort iamMembershipReferencePort,
            IamActiveMembershipByEmailPort iamActiveMembershipByEmailPort,
            IamSystemRoleReferencePort iamSystemRoleReferencePort,
            TenantAccessProvisionPort tenantAccessProvisionPort,
            SendInvitationEmailPort sendInvitationEmailPort,
            TransactionalOperator transactionalOperator
    ) {
        this(
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
                transactionalOperator,
                DEFAULT_INVITATION_TTL
        );
    }

    public InvitationAdministrationUseCaseImpl(
            TenantContextAccessor tenantContextAccessor,
            MembershipContextAccessor membershipContextAccessor,
            InvitationAdminQueryRepository invitationAdminQueryRepository,
            InvitationRepository invitationRepository,
            InvitationQueryPort invitationQueryPort,
            IamMembershipReferencePort iamMembershipReferencePort,
            IamActiveMembershipByEmailPort iamActiveMembershipByEmailPort,
            IamSystemRoleReferencePort iamSystemRoleReferencePort,
            TenantAccessProvisionPort tenantAccessProvisionPort,
            SendInvitationEmailPort sendInvitationEmailPort,
            TransactionalOperator transactionalOperator,
            Duration invitationTtl
    ) {
        this.tenantContextAccessor = Objects.requireNonNull(tenantContextAccessor, "tenantContextAccessor");
        this.membershipContextAccessor = Objects.requireNonNull(
                membershipContextAccessor,
                "membershipContextAccessor"
        );
        this.invitationAdminQueryRepository = Objects.requireNonNull(
                invitationAdminQueryRepository,
                "invitationAdminQueryRepository"
        );
        this.invitationRepository = Objects.requireNonNull(invitationRepository, "invitationRepository");
        this.invitationQueryPort = Objects.requireNonNull(invitationQueryPort, "invitationQueryPort");
        this.iamMembershipReferencePort = Objects.requireNonNull(
                iamMembershipReferencePort,
                "iamMembershipReferencePort"
        );
        this.iamActiveMembershipByEmailPort = Objects.requireNonNull(
                iamActiveMembershipByEmailPort,
                "iamActiveMembershipByEmailPort"
        );
        this.iamSystemRoleReferencePort = Objects.requireNonNull(
                iamSystemRoleReferencePort,
                "iamSystemRoleReferencePort"
        );
        this.tenantAccessProvisionPort = Objects.requireNonNull(
                tenantAccessProvisionPort,
                "tenantAccessProvisionPort"
        );
        this.sendInvitationEmailPort = Objects.requireNonNull(
                sendInvitationEmailPort,
                "sendInvitationEmailPort"
        );
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
        this.invitationTtl = Objects.requireNonNull(invitationTtl, "invitationTtl");
    }

    @Override
    public Mono<PagedResult<AdminInvitationView>> execute(InvitationListQuery filter, PageQuery pageQuery) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> invitationAdminQueryRepository.countByTenantId(tenantId, filter)
                        .flatMap(total -> invitationAdminQueryRepository
                                .findByTenantId(tenantId, filter, pageQuery)
                                .collectList()
                                .map(content -> PagedResult.of(
                                        content,
                                        pageQuery.page(),
                                        pageQuery.size(),
                                        total
                                ))));
    }

    @Override
    public Mono<AdminInvitationView> execute(InvitationId invitationId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, invitationId).map(this::toView));
    }

    @Override
    public Mono<CreateInvitationResult> execute(CreateInvitationCommand command) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> resolveInvitedByMembershipId(command)
                        .flatMap(invitedBy -> createInvitation(tenantId, invitedBy, command)))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminInvitationView> revoke(InvitationId invitationId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, invitationId)
                        .flatMap(invitation -> {
                            invitation.revoke(Instant.now());
                            return invitationRepository.save(invitation);
                        })
                        .map(this::toView))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AcceptInvitationResult> execute(AcceptInvitationCommand command) {
        Objects.requireNonNull(command, "command");
        if (command.rawToken() == null || command.rawToken().isBlank()) {
            return Mono.error(new InvalidDomainValueException("rawToken is required"));
        }

        Instant now = Instant.now();
        InvitationTokenHash tokenHash = InvitationTokenHash.ofHashedValue(
                InvitationTokenHasher.hash(command.rawToken().trim())
        );

        return invitationQueryPort.findByTokenHash(tokenHash)
                .switchIfEmpty(Mono.error(new InvitationNotFoundException("Invitation not found")))
                .flatMap(invitation -> acceptInvitation(invitation, command, now));
    }

    private Mono<CreateInvitationResult> createInvitation(
            TenantId tenantId,
            MembershipId invitedBy,
            CreateInvitationCommand command
    ) {
        EmailAddress email = EmailAddress.of(command.email());
        InvitationRoleCode roleCode = InvitationRoleCode.of(command.roleCode());
        Instant now = Instant.now();

        return validateInviterActive(tenantId, invitedBy)
                .then(Mono.defer(() -> validateNoActiveMembership(tenantId, email)))
                .then(Mono.defer(() -> validateNoPendingInvitation(tenantId, email)))
                .then(Mono.defer(() -> validateSystemRole(tenantId, roleCode)))
                .then(Mono.defer(() -> {
                    String rawToken = InvitationTokenHasher.generateRawToken();
                    InvitationTokenHash tokenHash = InvitationTokenHash.ofHashedValue(
                            InvitationTokenHasher.hash(rawToken)
                    );
                    Invitation invitation = Invitation.create(
                            InvitationId.generate(),
                            tenantId,
                            email,
                            roleCode,
                            invitedBy,
                            tokenHash,
                            now.plus(invitationTtl),
                            now
                    );
                    return invitationRepository.save(invitation)
                            .flatMap(saved -> sendInvitationEmailPort
                                    .send(tenantId, saved.id(), email, rawToken)
                                    .onErrorResume(ex -> Mono.empty())
                                    .thenReturn(new CreateInvitationResult(toView(saved), rawToken)));
                }));
    }

    private Mono<AcceptInvitationResult> acceptInvitation(
            Invitation invitation,
            AcceptInvitationCommand command,
            Instant now
    ) {
        if (invitation.status() != com.codecore.access.domain.valueobject.InvitationStatus.PENDING) {
            return Mono.error(new InvalidInvitationStateException(
                    "Cannot accept when invitation is " + invitation.status()
            ));
        }
        if (!now.isBefore(invitation.expiresAt())) {
            invitation.expire(now);
            // Commit EXPIRED before surfacing the error — an error inside the same TX would roll back.
            return invitationRepository.save(invitation)
                    .as(transactionalOperator::transactional)
                    .then(Mono.error(new InvalidInvitationStateException("Cannot accept an expired invitation")));
        }

        RawPassword passwordOrNull = command.password() == null || command.password().isBlank()
                ? null
                : RawPassword.of(command.password());

        TenantAccessProvisionPort.ProvisionTenantAccessCommand provisionCmd =
                new TenantAccessProvisionPort.ProvisionTenantAccessCommand(
                        new com.codecore.iam.domain.valueobject.TenantId(invitation.tenantId().value()),
                        com.codecore.iam.domain.valueobject.EmailAddress.of(invitation.invitedEmail().value()),
                        invitation.invitedRoleCode().value(),
                        passwordOrNull,
                        now
                );

        return tenantAccessProvisionPort.provision(provisionCmd)
                .flatMap(iamMembershipId -> {
                    MembershipId resulting = MembershipId.of(iamMembershipId.value());
                    invitation.accept(now, resulting);
                    return invitationRepository.save(invitation)
                            .map(saved -> new AcceptInvitationResult(toView(saved), resulting.value()));
                })
                .as(transactionalOperator::transactional);
    }

    private Mono<MembershipId> resolveInvitedByMembershipId(CreateInvitationCommand command) {
        if (command.invitedByMembershipId() != null) {
            return Mono.just(MembershipId.of(command.invitedByMembershipId()));
        }
        return membershipContextAccessor.currentMembershipId();
    }

    private Mono<Void> validateInviterActive(TenantId tenantId, MembershipId invitedBy) {
        return iamMembershipReferencePort.existsActiveByIdAndTenant(
                        new com.codecore.iam.domain.valueobject.MembershipId(invitedBy.value()),
                        new com.codecore.iam.domain.valueobject.TenantId(tenantId.value())
                )
                .flatMap(exists -> exists
                        ? Mono.<Void>empty()
                        : Mono.error(new InviterMembershipNotFoundException(
                                "Inviter membership not found, not in tenant, or not ACTIVE")));
    }

    private Mono<Void> validateNoActiveMembership(TenantId tenantId, EmailAddress email) {
        return iamActiveMembershipByEmailPort.existsActiveByEmailAndTenant(
                        com.codecore.iam.domain.valueobject.EmailAddress.of(email.value()),
                        new com.codecore.iam.domain.valueobject.TenantId(tenantId.value())
                )
                .flatMap(exists -> exists
                        ? Mono.error(new ActiveMembershipAlreadyExistsException(
                                "Active membership already exists for email in tenant"))
                        : Mono.<Void>empty());
    }

    private Mono<Void> validateNoPendingInvitation(TenantId tenantId, EmailAddress email) {
        return invitationQueryPort.existsPendingByEmailAndTenant(email, tenantId)
                .flatMap(exists -> exists
                        ? Mono.error(new PendingInvitationAlreadyExistsException(
                                "Pending invitation already exists for email in tenant"))
                        : Mono.<Void>empty());
    }

    private Mono<Void> validateSystemRole(TenantId tenantId, InvitationRoleCode roleCode) {
        return iamSystemRoleReferencePort.existsSystemRoleByCodeAndTenant(
                        roleCode.value(),
                        new com.codecore.iam.domain.valueobject.TenantId(tenantId.value())
                )
                .flatMap(exists -> exists
                        ? Mono.<Void>empty()
                        : Mono.error(new SystemRoleNotFoundException(
                                "System role not found for code: " + roleCode.value())));
    }

    private Mono<Invitation> loadInTenant(TenantId tenantId, InvitationId invitationId) {
        return invitationQueryPort.findByIdAndTenantId(invitationId, tenantId)
                .switchIfEmpty(Mono.error(new InvitationNotFoundException(
                        "Invitation not found in tenant context")));
    }

    private AdminInvitationView toView(Invitation invitation) {
        return new AdminInvitationView(
                invitation.id(),
                invitation.tenantId(),
                invitation.invitedEmail().value(),
                invitation.invitedRoleCode().value(),
                invitation.invitedByMembershipId(),
                invitation.expiresAt(),
                invitation.status(),
                invitation.resultingMembershipId().orElse(null),
                invitation.createdAt(),
                invitation.updatedAt(),
                invitation.acceptedAt().orElse(null),
                invitation.revokedAt().orElse(null)
        );
    }
}
