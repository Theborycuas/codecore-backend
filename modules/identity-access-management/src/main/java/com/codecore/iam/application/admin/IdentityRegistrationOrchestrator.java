package com.codecore.iam.application.admin;

import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.valueobject.CredentialId;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.RawPassword;
import com.codecore.iam.domain.valueobject.TenantId;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Shared identity + membership registration (bootstrap and admin create).
 */
public final class IdentityRegistrationOrchestrator {

    private final IdentityRepository identityRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordHasher passwordHasher;
    private final TransactionalOperator transactionalOperator;

    public IdentityRegistrationOrchestrator(
            IdentityRepository identityRepository,
            MembershipRepository membershipRepository,
            PasswordHasher passwordHasher,
            TransactionalOperator transactionalOperator
    ) {
        this.identityRepository = Objects.requireNonNull(identityRepository, "identityRepository");
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "membershipRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    public Mono<Identity> registerNewIdentity(
            TenantId tenantId,
            EmailAddress email,
            RawPassword rawPassword,
            IdentityStatus initialStatus
    ) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(rawPassword, "rawPassword");
        Objects.requireNonNull(initialStatus, "initialStatus");

        IdentityId identityId = IdentityId.generate();
        Instant now = Instant.now();
        String hashed = passwordHasher.hash(rawPassword.value());
        PasswordHash passwordHash = PasswordHash.ofHashedValue(hashed);

        Credential credential = new Credential(
                new CredentialId(identityId.value()),
                passwordHash,
                now,
                null,
                false,
                0L
        );

        Identity identity = new Identity(
                identityId,
                tenantId,
                email,
                initialStatus,
                credential,
                null,
                now,
                now,
                0L
        );

        Mono<Identity> registration = identityRepository.save(identity)
                .flatMap(saved -> {
                    IdentityTenantMembership membership = IdentityTenantMembership.create(
                            saved.id(),
                            tenantId,
                            now
                    );
                    return membershipRepository.save(membership).thenReturn(saved);
                });

        return transactionalOperator.transactional(registration);
    }
}
