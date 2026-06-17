package com.codecore.iam.application.admin;

import com.codecore.iam.application.command.CreateAdminUserCommand;
import com.codecore.iam.application.command.UpdateAdminUserCommand;
import com.codecore.iam.application.dto.AdminUserView;
import com.codecore.iam.application.dto.AuthorizationContext;
import com.codecore.iam.application.dto.PagedResult;
import com.codecore.iam.application.port.in.CreateAdminUserUseCase;
import com.codecore.iam.application.port.in.DeactivateAdminUserUseCase;
import com.codecore.iam.application.port.in.GetAdminUserUseCase;
import com.codecore.iam.application.port.in.ListAdminUsersUseCase;
import com.codecore.iam.application.port.in.UpdateAdminUserUseCase;
import com.codecore.iam.application.port.out.AuthorizationContextAccessor;
import com.codecore.iam.application.port.out.IdentityAdminQueryRepository;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.query.PageQuery;
import com.codecore.iam.domain.exception.IdentityAlreadyExistsException;
import com.codecore.iam.domain.exception.IdentityNotFoundException;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.RawPassword;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * User administration use cases (FASE 15.1).
 */
public final class UserAdministrationUseCaseImpl
        implements ListAdminUsersUseCase,
        GetAdminUserUseCase,
        CreateAdminUserUseCase,
        UpdateAdminUserUseCase,
        DeactivateAdminUserUseCase {

    private final AuthorizationContextAccessor authorizationContextAccessor;
    private final IdentityAdminQueryRepository identityAdminQueryRepository;
    private final IdentityRepository identityRepository;
    private final MembershipRepository membershipRepository;
    private final IdentityRegistrationOrchestrator registrationOrchestrator;
    private final OwnershipPolicy ownershipPolicy;
    private final TransactionalOperator transactionalOperator;

    public UserAdministrationUseCaseImpl(
            AuthorizationContextAccessor authorizationContextAccessor,
            IdentityAdminQueryRepository identityAdminQueryRepository,
            IdentityRepository identityRepository,
            MembershipRepository membershipRepository,
            IdentityRegistrationOrchestrator registrationOrchestrator,
            OwnershipPolicy ownershipPolicy,
            TransactionalOperator transactionalOperator
    ) {
        this.authorizationContextAccessor = Objects.requireNonNull(
                authorizationContextAccessor,
                "authorizationContextAccessor"
        );
        this.identityAdminQueryRepository = Objects.requireNonNull(
                identityAdminQueryRepository,
                "identityAdminQueryRepository"
        );
        this.identityRepository = Objects.requireNonNull(identityRepository, "identityRepository");
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "membershipRepository");
        this.registrationOrchestrator = Objects.requireNonNull(
                registrationOrchestrator,
                "registrationOrchestrator"
        );
        this.ownershipPolicy = Objects.requireNonNull(ownershipPolicy, "ownershipPolicy");
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<PagedResult<AdminUserView>> execute(PageQuery pageQuery) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> identityAdminQueryRepository.countByTenantMembership(ctx.tenantId())
                        .flatMap(total -> identityAdminQueryRepository
                                .findByTenantMembership(ctx.tenantId(), pageQuery)
                                .map(this::toView)
                                .collectList()
                                .map(content -> PagedResult.of(
                                        content,
                                        pageQuery.page(),
                                        pageQuery.size(),
                                        total
                                ))));
    }

    @Override
    public Mono<AdminUserView> execute(IdentityId identityId) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadIdentityInTenant(ctx, identityId).map(this::toView));
    }

    @Override
    public Mono<AdminUserView> execute(CreateAdminUserCommand command) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> {
                    validateCreate(command);
                    EmailAddress email = EmailAddress.of(command.email());
                    RawPassword password = RawPassword.of(command.rawPassword());
                    IdentityStatus status = command.initialStatus() != null
                            ? command.initialStatus()
                            : IdentityStatus.ACTIVE;

                    return identityRepository.existsByEmail(email)
                            .flatMap(exists -> {
                                if (exists) {
                                    return Mono.error(new IdentityAlreadyExistsException(
                                            "Identity already exists for this email"));
                                }
                                return registrationOrchestrator.registerNewIdentity(
                                                ctx.tenantId(),
                                                email,
                                                password,
                                                status
                                        )
                                        .map(this::toView);
                            });
                });
    }

    @Override
    public Mono<AdminUserView> execute(UpdateAdminUserCommand command) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadIdentityInTenant(ctx, command.identityId())
                        .flatMap(identity -> ownershipPolicy.assertCanModifyUser(ctx, command.identityId())
                                .then(applyUpdates(identity, command)))
                        .flatMap(identityRepository::save)
                        .map(this::toView)
                        .as(transactionalOperator::transactional));
    }

    @Override
    public Mono<Void> deactivate(IdentityId identityId) {
        return authorizationContextAccessor.current()
                .flatMap(ctx -> loadIdentityInTenant(ctx, identityId)
                        .flatMap(identity -> ownershipPolicy.assertCanModifyUser(ctx, identityId)
                                .then(Mono.defer(() -> {
                                    identity.disable();
                                    return identityRepository.save(identity);
                                })))
                        .then()
                        .as(transactionalOperator::transactional));
    }

    private Mono<Identity> loadIdentityInTenant(AuthorizationContext ctx, IdentityId identityId) {
        return membershipRepository.exists(identityId, ctx.tenantId())
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IdentityNotFoundException(
                                "User not found in tenant context"));
                    }
                    return identityRepository.findById(identityId);
                })
                .switchIfEmpty(Mono.error(new IdentityNotFoundException(
                        "User not found in tenant context")));
    }

    private Mono<Identity> applyUpdates(Identity identity, UpdateAdminUserCommand command) {
        if (command.status() != null) {
            applyStatus(identity, command.status());
        }
        if (command.email() != null && !command.email().isBlank()) {
            EmailAddress newEmail = EmailAddress.of(command.email());
            if (!newEmail.equals(identity.email())) {
                return identityRepository.existsByEmail(newEmail)
                        .flatMap(exists -> {
                            if (exists) {
                                return Mono.error(new IdentityAlreadyExistsException(
                                        "Identity already exists for this email"));
                            }
                            identity.changeEmail(newEmail);
                            return Mono.just(identity);
                        });
            }
        }
        return Mono.just(identity);
    }

    private static void applyStatus(Identity identity, IdentityStatus status) {
        switch (status) {
            case ACTIVE -> identity.enable();
            case DISABLED -> identity.disable();
            case LOCKED -> identity.lockAccount();
            case PENDING_VERIFICATION -> throw new InvalidDomainValueException(
                    "Cannot set status to PENDING_VERIFICATION via admin update");
            case PASSWORD_RESET_REQUIRED -> identity.requirePasswordReset();
        }
    }

    private static void validateCreate(CreateAdminUserCommand command) {
        if (command.email() == null || command.email().isBlank()) {
            throw new InvalidDomainValueException("Email must not be blank");
        }
        if (command.rawPassword() == null || command.rawPassword().isBlank()) {
            throw new InvalidDomainValueException("Password must not be blank");
        }
    }

    private AdminUserView toView(Identity identity) {
        return new AdminUserView(
                identity.id(),
                identity.email().value(),
                identity.status(),
                identity.lastLoginAt(),
                identity.createdAt(),
                identity.updatedAt()
        );
    }
}
