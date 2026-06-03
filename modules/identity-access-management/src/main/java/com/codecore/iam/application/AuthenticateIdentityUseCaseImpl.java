package com.codecore.iam.application;

import com.codecore.iam.application.command.AuthenticationCommand;
import com.codecore.iam.application.dto.AuthenticationResult;
import com.codecore.iam.application.port.in.AuthenticateIdentityUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.domain.exception.IdentityNotAllowedToAuthenticateException;
import com.codecore.iam.domain.exception.InvalidCredentialsException;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Validates tenant-scoped credentials against persisted identity — no tokens or sessions.
 */
public class AuthenticateIdentityUseCaseImpl implements AuthenticateIdentityUseCase {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";
    private static final String NOT_ALLOWED_MESSAGE = "Identity is not allowed to authenticate";

    private final IdentityRepository identityRepository;
    private final PasswordHasher passwordHasher;

    public AuthenticateIdentityUseCaseImpl(IdentityRepository identityRepository, PasswordHasher passwordHasher) {
        this.identityRepository = Objects.requireNonNull(identityRepository, "identityRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
    }

    @Override
    public Mono<AuthenticationResult> execute(AuthenticationCommand command) {
        return Mono.defer(() -> {
            Objects.requireNonNull(command, "command");
            Objects.requireNonNull(command.tenantId(), "tenantId");

            validateNotBlank(command.email(), "Email must not be blank");
            validateNotBlank(command.rawPassword(), "Password must not be blank");

            EmailAddress email = EmailAddress.of(command.email());
            String rawPassword = command.rawPassword();

            return identityRepository.findByTenantAndEmail(command.tenantId(), email)
                    .switchIfEmpty(Mono.error(new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE)))
                    .flatMap(identity -> authenticate(identity, rawPassword));
        });
    }

    private Mono<AuthenticationResult> authenticate(Identity identity, String rawPassword) {
        if (identity.status() != IdentityStatus.ACTIVE) {
            return Mono.error(new IdentityNotAllowedToAuthenticateException(NOT_ALLOWED_MESSAGE));
        }

        Credential credential = identity.credential()
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordHasher.matches(rawPassword, credential.passwordHash().value())) {
            return Mono.error(new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE));
        }

        return Mono.just(new AuthenticationResult(
                identity.id(),
                identity.tenantId(),
                identity.email(),
                identity.status()
        ));
    }

    private static void validateNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainValueException(message);
        }
    }
}
