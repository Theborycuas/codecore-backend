package com.codecore.iam.application;

import com.codecore.iam.application.admin.IdentityRegistrationOrchestrator;
import com.codecore.iam.application.authorization.SystemRoleTemplate;
import com.codecore.iam.application.command.CreateTenantCommand;
import com.codecore.iam.application.dto.BootstrapPlatformResult;
import com.codecore.iam.application.dto.CreateTenantResponse;
import com.codecore.iam.application.port.in.BootstrapPlatformUseCase;
import com.codecore.iam.application.port.in.CreateTenantUseCase;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.MembershipRoleRepository;
import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.configuration.PlatformBootstrapProperties;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.model.membership.MembershipRoleAssignment;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.RawPassword;
import com.codecore.iam.domain.valueobject.TenantId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Orchestrates first-tenant bootstrap when enabled and database is empty (PASO 15.9.2).
 */
public final class BootstrapPlatformUseCaseImpl implements BootstrapPlatformUseCase {

    private static final Logger log = LoggerFactory.getLogger(BootstrapPlatformUseCaseImpl.class);

    private final PlatformBootstrapProperties properties;
    private final TenantRepository tenantRepository;
    private final CreateTenantUseCase createTenantUseCase;
    private final IdentityRegistrationOrchestrator identityRegistrationOrchestrator;
    private final MembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final MembershipRoleRepository membershipRoleRepository;

    public BootstrapPlatformUseCaseImpl(
            PlatformBootstrapProperties properties,
            TenantRepository tenantRepository,
            CreateTenantUseCase createTenantUseCase,
            IdentityRegistrationOrchestrator identityRegistrationOrchestrator,
            MembershipRepository membershipRepository,
            RoleRepository roleRepository,
            MembershipRoleRepository membershipRoleRepository
    ) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
        this.createTenantUseCase = Objects.requireNonNull(createTenantUseCase, "createTenantUseCase");
        this.identityRegistrationOrchestrator = Objects.requireNonNull(
                identityRegistrationOrchestrator,
                "identityRegistrationOrchestrator"
        );
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "membershipRepository");
        this.roleRepository = Objects.requireNonNull(roleRepository, "roleRepository");
        this.membershipRoleRepository = Objects.requireNonNull(
                membershipRoleRepository,
                "membershipRoleRepository"
        );
    }

    @Override
    public Mono<BootstrapPlatformResult> executeIfNeeded() {
        if (!properties.isEnabled()) {
            return Mono.just(BootstrapPlatformResult.skipped());
        }
        return tenantRepository.count()
                .flatMap(count -> {
                    if (count > 0) {
                        log.info("Platform bootstrap skipped: {} tenant(s) already exist", count);
                        return Mono.just(BootstrapPlatformResult.skipped());
                    }
                    return executeBootstrap();
                });
    }

    private Mono<BootstrapPlatformResult> executeBootstrap() {
        validateBootstrapConfig();
        String tenantName = properties.getTenantName().trim();
        EmailAddress ownerEmail = EmailAddress.of(properties.getOwnerEmail().trim());
        RawPassword ownerPassword = RawPassword.of(properties.getOwnerPassword());

        log.info("Executing platform bootstrap for tenant '{}'", tenantName);

        return createTenantUseCase.execute(new CreateTenantCommand(tenantName))
                .flatMap(tenantResponse -> registerOwner(tenantResponse, ownerEmail, ownerPassword))
                .doOnSuccess(result -> log.info(
                        "Platform bootstrap completed: tenantId={} owner={}",
                        result.tenantId().value(),
                        result.ownerEmail()
                ));
    }

    private Mono<BootstrapPlatformResult> registerOwner(
            CreateTenantResponse tenantResponse,
            EmailAddress ownerEmail,
            RawPassword ownerPassword
    ) {
        TenantId tenantId = tenantResponse.tenantId();
        return identityRegistrationOrchestrator.registerNewIdentity(
                        tenantId,
                        ownerEmail,
                        ownerPassword,
                        IdentityStatus.ACTIVE
                )
                .flatMap(identity -> assignOwnerRole(tenantId, identity, ownerEmail, tenantResponse.name().value()));
    }

    private Mono<BootstrapPlatformResult> assignOwnerRole(
            TenantId tenantId,
            Identity identity,
            EmailAddress ownerEmail,
            String tenantName
    ) {
        return membershipRepository.findByIdentityIdAndTenantId(identity.id(), tenantId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Bootstrap membership missing")))
                .flatMap(membership -> roleRepository.findByTenantIdAndCode(tenantId, SystemRoleTemplate.OWNER.code())
                        .switchIfEmpty(Mono.error(new IllegalStateException("Bootstrap OWNER role missing")))
                        .flatMap(ownerRole -> membershipRoleRepository.assign(
                                membership.id(),
                                MembershipRoleAssignment.assign(ownerRole.id(), Instant.now())
                        ).thenReturn(completed(tenantId, tenantName, ownerEmail.value()))));
    }

    private BootstrapPlatformResult completed(TenantId tenantId, String tenantName, String ownerEmail) {
        return BootstrapPlatformResult.completed(tenantId, tenantName, ownerEmail);
    }

    private void validateBootstrapConfig() {
        if (properties.getTenantName() == null || properties.getTenantName().isBlank()) {
            throw new InvalidDomainValueException("codecore.platform.bootstrap.tenant-name is required when enabled");
        }
        if (properties.getOwnerEmail() == null || properties.getOwnerEmail().isBlank()) {
            throw new InvalidDomainValueException("codecore.platform.bootstrap.owner-email is required when enabled");
        }
        if (properties.getOwnerPassword() == null || properties.getOwnerPassword().isBlank()) {
            throw new InvalidDomainValueException("codecore.platform.bootstrap.owner-password is required when enabled");
        }
    }
}
