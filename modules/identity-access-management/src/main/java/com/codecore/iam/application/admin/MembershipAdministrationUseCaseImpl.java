package com.codecore.iam.application.admin;

import com.codecore.iam.application.command.CreateAdminMembershipCommand;
import com.codecore.iam.application.command.UpdateAdminMembershipCommand;
import com.codecore.iam.application.dto.AdminMembershipView;
import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.dto.PagedResult;
import com.codecore.iam.application.port.in.CreateAdminMembershipUseCase;
import com.codecore.iam.application.port.in.DeactivateAdminMembershipUseCase;
import com.codecore.iam.application.port.in.GetAdminMembershipUseCase;
import com.codecore.iam.application.port.in.ListAdminMembershipsUseCase;
import com.codecore.iam.application.port.in.UpdateAdminMembershipUseCase;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipAdminQueryRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.query.PageQuery;
import com.codecore.iam.domain.exception.IdentityNotFoundException;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.exception.MembershipAlreadyExistsException;
import com.codecore.iam.domain.exception.MembershipNotFoundException;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.RawPassword;
import com.codecore.iam.domain.valueobject.TenantId;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Membership administration use cases (FASE 15.2).
 */
public final class MembershipAdministrationUseCaseImpl
        implements ListAdminMembershipsUseCase,
        GetAdminMembershipUseCase,
        CreateAdminMembershipUseCase,
        UpdateAdminMembershipUseCase,
        DeactivateAdminMembershipUseCase {

    private final AuthorizationContextAccessor authorizationContextAccessor;
    private final MembershipAdminQueryRepository membershipAdminQueryRepository;
    private final MembershipRepository membershipRepository;
    private final IdentityRepository identityRepository;
    private final IdentityRegistrationOrchestrator registrationOrchestrator;
    private final OwnershipPolicy ownershipPolicy;
    private final TransactionalOperator transactionalOperator;

    public MembershipAdministrationUseCaseImpl(
            AuthorizationContextAccessor authorizationContextAccessor,
            MembershipAdminQueryRepository membershipAdminQueryRepository,
            MembershipRepository membershipRepository,
            IdentityRepository identityRepository,
            IdentityRegistrationOrchestrator registrationOrchestrator,
            OwnershipPolicy ownershipPolicy,
            TransactionalOperator transactionalOperator
    ) {
        this.authorizationContextAccessor = Objects.requireNonNull(
                authorizationContextAccessor,
                "authorizationContextAccessor"
        );
        this.membershipAdminQueryRepository = Objects.requireNonNull(
                membershipAdminQueryRepository,
                "membershipAdminQueryRepository"
        );
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "membershipRepository");
        this.identityRepository = Objects.requireNonNull(identityRepository, "identityRepository");
        this.registrationOrchestrator = Objects.requireNonNull(
                registrationOrchestrator,
                "registrationOrchestrator"
        );
        this.ownershipPolicy = Objects.requireNonNull(ownershipPolicy, "ownershipPolicy");
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<PagedResult<AdminMembershipView>> execute(PageQuery pageQuery) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> membershipAdminQueryRepository.countByTenantId(ctx.tenantId())
                        .flatMap(total -> membershipAdminQueryRepository
                                .findByTenantId(ctx.tenantId(), pageQuery)
                                .collectList()
                                .map(content -> PagedResult.of(
                                        content,
                                        pageQuery.page(),
                                        pageQuery.size(),
                                        total
                                ))));
    }

    @Override
    public Mono<AdminMembershipView> execute(MembershipId membershipId) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadMembershipInTenant(ctx.tenantId(), membershipId)
                        .flatMap(membership -> toView(membership)));
    }

    @Override
    public Mono<AdminMembershipView> execute(CreateAdminMembershipCommand command) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> {
                    if (hasPassword(command)) {
                        return createWithNewIdentity(ctx.tenantId(), command);
                    }
                    return resolveIdentityId(command)
                            .flatMap(identityId -> linkExistingIdentity(ctx.tenantId(), identityId));
                })
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<AdminMembershipView> execute(UpdateAdminMembershipCommand command) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadMembershipInTenant(ctx.tenantId(), command.membershipId())
                        .flatMap(membership -> ownershipPolicy
                                .assertCanModifyUser(ctx, membership.identityId())
                                .then(applyStatus(membership, command.status()))
                                .flatMap(membershipRepository::save)
                                .flatMap(this::toView)))
                .as(transactionalOperator::transactional);
    }

    @Override
    public Mono<Void> deactivate(MembershipId membershipId) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadMembershipInTenant(ctx.tenantId(), membershipId)
                        .flatMap(membership -> ownershipPolicy
                                .assertCanModifyUser(ctx, membership.identityId())
                                .then(Mono.defer(() -> {
                                    membership.deactivate();
                                    return membershipRepository.save(membership);
                                })))
                        .then())
                .as(transactionalOperator::transactional);
    }

    private Mono<AdminMembershipView> createWithNewIdentity(
            TenantId tenantId,
            CreateAdminMembershipCommand command
    ) {
        validateEmailAndPassword(command);
        EmailAddress email = EmailAddress.of(command.email());
        RawPassword password = RawPassword.of(command.password());

        return identityRepository.existsByEmail(email)
                .flatMap(exists -> {
                    if (exists) {
                        return identityRepository.findByEmail(email)
                                .flatMap(identity -> ensureNoMembershipThenLink(tenantId, identity.id()));
                    }
                    return registrationOrchestrator.registerNewIdentity(
                                    tenantId,
                                    email,
                                    password,
                                    IdentityStatus.ACTIVE
                            )
                            .flatMap(identity -> membershipRepository.findByIdentityIdAndTenantId(
                                    identity.id(),
                                    tenantId
                            ))
                            .switchIfEmpty(Mono.error(new MembershipNotFoundException(
                                    "Membership was not created for new identity")))
                            .flatMap(this::toView);
                });
    }

    private Mono<AdminMembershipView> linkExistingIdentity(TenantId tenantId, IdentityId identityId) {
        return identityRepository.findById(identityId)
                .switchIfEmpty(Mono.error(new IdentityNotFoundException("Identity not found")))
                .flatMap(identity -> ensureNoMembershipThenLink(tenantId, identity.id()));
    }

    private Mono<AdminMembershipView> ensureNoMembershipThenLink(TenantId tenantId, IdentityId identityId) {
        return membershipRepository.exists(identityId, tenantId)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new MembershipAlreadyExistsException(
                                "Membership already exists for identity in tenant"));
                    }
                    Instant now = Instant.now();
                    IdentityTenantMembership membership = IdentityTenantMembership.create(
                            identityId,
                            tenantId,
                            now
                    );
                    return membershipRepository.save(membership).flatMap(this::toView);
                });
    }

    private Mono<IdentityId> resolveIdentityId(CreateAdminMembershipCommand command) {
        if (command.identityIdValue() != null) {
            return Mono.just(command.identityIdValue());
        }
        if (command.email() != null && !command.email().isBlank()) {
            EmailAddress email = EmailAddress.of(command.email());
            return identityRepository.findByEmail(email)
                    .map(Identity::id)
                    .switchIfEmpty(Mono.error(new IdentityNotFoundException("Identity not found for email")));
        }
        return Mono.error(new InvalidDomainValueException(
                "identityId, email, or email+password required"));
    }

    private Mono<IdentityTenantMembership> loadMembershipInTenant(
            TenantId tenantId,
            MembershipId membershipId
    ) {
        return membershipRepository.findByIdAndTenantId(membershipId, tenantId)
                .switchIfEmpty(Mono.error(new MembershipNotFoundException(
                        "Membership not found in tenant context")));
    }

    private Mono<IdentityTenantMembership> applyStatus(
            IdentityTenantMembership membership,
            MembershipStatus status
    ) {
        if (status == null) {
            return Mono.error(new InvalidDomainValueException("status is required"));
        }
        if (status == MembershipStatus.ACTIVE) {
            membership.activate();
        } else if (status == MembershipStatus.INACTIVE) {
            membership.deactivate();
        } else {
            return Mono.error(new InvalidDomainValueException("Unsupported membership status"));
        }
        return Mono.just(membership);
    }

    private Mono<AdminMembershipView> toView(IdentityTenantMembership membership) {
        return identityRepository.findById(membership.identityId())
                .map(identity -> new AdminMembershipView(
                        membership.id(),
                        membership.identityId(),
                        membership.tenantId(),
                        membership.status(),
                        identity.email().value(),
                        membership.createdAt(),
                        membership.updatedAt()
                ))
                .switchIfEmpty(Mono.error(new IdentityNotFoundException("Identity not found for membership")));
    }

    private static boolean hasPassword(CreateAdminMembershipCommand command) {
        return command.password() != null && !command.password().isBlank();
    }

    private static void validateEmailAndPassword(CreateAdminMembershipCommand command) {
        if (command.email() == null || command.email().isBlank()) {
            throw new InvalidDomainValueException("Email must not be blank when creating identity");
        }
        if (command.password() == null || command.password().isBlank()) {
            throw new InvalidDomainValueException("Password must not be blank when creating identity");
        }
    }
}
