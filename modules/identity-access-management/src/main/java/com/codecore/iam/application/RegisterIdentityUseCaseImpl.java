package com.codecore.iam.application;

import com.codecore.iam.application.admin.IdentityRegistrationOrchestrator;
import com.codecore.iam.application.command.RegisterIdentityCommand;
import com.codecore.iam.application.dto.RegisterIdentityResult;
import com.codecore.iam.application.port.in.RegisterIdentityUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.domain.exception.IdentityAlreadyExistsException;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.RawPassword;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Registers a new {@link Identity} for a tenant (email-first, no HTTP/JWT).
 */
public class RegisterIdentityUseCaseImpl implements RegisterIdentityUseCase {

    private final IdentityRepository identityRepository;
    private final IdentityRegistrationOrchestrator registrationOrchestrator;

    public RegisterIdentityUseCaseImpl(
            IdentityRepository identityRepository,
            IdentityRegistrationOrchestrator registrationOrchestrator
    ) {
        this.identityRepository = Objects.requireNonNull(identityRepository, "identityRepository");
        this.registrationOrchestrator = Objects.requireNonNull(
                registrationOrchestrator,
                "registrationOrchestrator"
        );
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

            return identityRepository.existsByEmail(email)
                    .flatMap(exists -> {
                        if (exists) {
                            return Mono.error(new IdentityAlreadyExistsException(
                                    "Identity already exists for this email"));
                        }
                        return registrationOrchestrator.registerNewIdentity(
                                        command.tenantId(),
                                        email,
                                        rawPassword,
                                        IdentityStatus.PENDING_VERIFICATION
                                )
                                .map(saved -> new RegisterIdentityResult(
                                        saved.id(),
                                        saved.tenantId(),
                                        saved.email(),
                                        saved.status()
                                ));
                    });
        });
    }

    private static void validateNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainValueException(message);
        }
    }
}
