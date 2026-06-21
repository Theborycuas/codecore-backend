package com.codecore.iam.application;

import com.codecore.iam.application.command.AuthenticationCommand;
import com.codecore.iam.application.dto.AccessTokenClaims;
import com.codecore.iam.application.dto.AuthenticationResponse;
import com.codecore.iam.application.port.in.AuthenticateIdentityUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.application.port.out.TokenProvider;
import com.codecore.iam.domain.exception.IdentityNotAllowedToAuthenticateException;
import com.codecore.iam.domain.exception.IdentityNotMemberOfTenantException;
import com.codecore.iam.domain.exception.InvalidCredentialsException;
import com.codecore.iam.domain.exception.InvalidDomainValueException;
import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.MembershipStatus;
import com.codecore.iam.domain.valueobject.TenantId;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Validates credentials and issues an access JWT via {@link TokenProvider} — no sessions or refresh tokens.
 */
public class AuthenticateIdentityUseCaseImpl implements AuthenticateIdentityUseCase {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";
    private static final String NOT_ALLOWED_MESSAGE = "Identity is not allowed to authenticate";
    private static final String NOT_MEMBER_MESSAGE = "Identity is not a member of this tenant";

    private final IdentityRepository identityRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final TenantOperationalGuard tenantOperationalGuard;

    public AuthenticateIdentityUseCaseImpl(
            IdentityRepository identityRepository,
            MembershipRepository membershipRepository,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider,
            TenantOperationalGuard tenantOperationalGuard
    ) {
        this.identityRepository = Objects.requireNonNull(identityRepository, "identityRepository");
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "membershipRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
        this.tenantOperationalGuard = Objects.requireNonNull(
                tenantOperationalGuard,
                "tenantOperationalGuard"
        );
    }

    @Override
    public Mono<AuthenticationResponse> execute(AuthenticationCommand command) {
        return Mono.defer(() -> {
            Objects.requireNonNull(command, "command");
            Objects.requireNonNull(command.tenantId(), "tenantId");

            validateNotBlank(command.email(), "Email must not be blank");
            validateNotBlank(command.rawPassword(), "Password must not be blank");

            EmailAddress email = EmailAddress.of(command.email());
            String rawPassword = command.rawPassword();

            return identityRepository.findByEmail(email)
                    .switchIfEmpty(Mono.error(new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE)))
                    .flatMap(identity -> authenticate(identity, command.tenantId(), rawPassword));
        });
    }

    private Mono<AuthenticationResponse> authenticate(
            Identity identity,
            TenantId tenantId,
            String rawPassword
    ) {
        if (identity.status() != IdentityStatus.ACTIVE) {
            return Mono.error(new IdentityNotAllowedToAuthenticateException(NOT_ALLOWED_MESSAGE, identity.status()));
        }

        Credential credential = identity.credential()
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordHasher.matches(rawPassword, credential.passwordHash().value())) {
            return Mono.error(new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE));
        }

        return requireActiveMembership(identity, tenantId)
                .then(tenantOperationalGuard.assertOperational(tenantId))
                .then(Mono.fromCallable(() -> tokenProvider.generateAccessToken(new AccessTokenClaims(
                identity.id().value().toString(),
                identity.email().value(),
                identity.status().name(),
                tenantId.value().toString()
                )))).map(issued -> new AuthenticationResponse(
                issued.accessToken(),
                issued.tokenType(),
                issued.expiresIn()
        ));
    }

    private Mono<IdentityTenantMembership> requireActiveMembership(Identity identity, TenantId tenantId) {
        return membershipRepository.findByIdentityId(identity.id())
                .filter(membership -> membership.tenantId().equals(tenantId))
                .next()
                .switchIfEmpty(Mono.error(new IdentityNotMemberOfTenantException(NOT_MEMBER_MESSAGE)))
                .flatMap(membership -> {
                    if (membership.status() != MembershipStatus.ACTIVE) {
                        return Mono.error(new IdentityNotMemberOfTenantException(NOT_MEMBER_MESSAGE));
                    }
                    return Mono.just(membership);
                });
    }

    private static void validateNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainValueException(message);
        }
    }
}
