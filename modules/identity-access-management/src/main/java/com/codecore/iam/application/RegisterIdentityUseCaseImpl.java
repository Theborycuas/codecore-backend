package com.codecore.iam.application;

import com.codecore.iam.application.command.RegisterIdentityCommand;
import com.codecore.iam.application.dto.RegisterIdentityResult;
import com.codecore.iam.application.port.in.RegisterIdentityUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.exception.IdentityAlreadyExistsException;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.valueobject.CredentialId;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.RawPassword;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Registers a new {@link Identity} for a tenant (email-first, no HTTP/JWT).
 */
public class RegisterIdentityUseCaseImpl implements RegisterIdentityUseCase {

    private final IdentityRepository identityRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordHasher passwordHasher;
    private final TransactionalOperator transactionalOperator;

    public RegisterIdentityUseCaseImpl(
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

    @Override
    public Mono<RegisterIdentityResult> execute(RegisterIdentityCommand command) {
        return Mono.defer(() -> {
            Objects.requireNonNull(command, "command");
            Objects.requireNonNull(command.tenantId(), "tenantId");

            validateNotBlank(command.email(), "Email must not be blank");
            validateNotBlank(command.rawPassword(), "Password must not be blank");

            EmailAddress email = EmailAddress.of(command.email());
            RawPassword rawPassword = RawPassword.of(command.rawPassword());

            return identityRepository.existsByTenantAndEmail(command.tenantId(), email)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IdentityAlreadyExistsException(
                                "Identity already exists for this tenant and email"));
                    }
                    return register(command.tenantId(), email, rawPassword);
                });
        });
    }

    private Mono<RegisterIdentityResult> register(
            com.codecore.iam.domain.valueobject.TenantId tenantId,
            EmailAddress email,
            RawPassword rawPassword
    ) {
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
                IdentityStatus.PENDING_VERIFICATION,
                credential,
                null,
                now,
                now,
                0L
        );

        Mono<RegisterIdentityResult> registration = identityRepository.save(identity)
                .flatMap(saved -> {
                    IdentityTenantMembership membership = IdentityTenantMembership.create(
                            saved.id(),
                            saved.tenantId(),
                            now
                    );
                    return membershipRepository.save(membership).thenReturn(saved);
                })
                .map(saved -> new RegisterIdentityResult(
                        saved.id(),
                        saved.tenantId(),
                        saved.email(),
                        saved.status()
                ));

        return transactionalOperator.transactional(registration);
    }

    private static void validateNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainValueException(message);
        }
    }
}
