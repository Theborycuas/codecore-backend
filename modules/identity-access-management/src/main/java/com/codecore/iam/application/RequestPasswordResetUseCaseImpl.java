package com.codecore.iam.application;

import com.codecore.iam.application.command.RequestPasswordResetCommand;
import com.codecore.iam.application.port.in.RequestPasswordResetUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.PasswordResetRepository;
import com.codecore.iam.application.port.out.SendPasswordResetEmailPort;
import com.codecore.iam.domain.model.passwordreset.PasswordResetRequest;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.ResetTokenHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TokenExpiration;
import com.codecore.iam.infrastructure.security.Sha256TokenHasher;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Starts password recovery. Always completes successfully to avoid email enumeration.
 */
public final class RequestPasswordResetUseCaseImpl implements RequestPasswordResetUseCase {

    static final Duration TOKEN_TTL = Duration.ofHours(1);

    private final IdentityRepository identityRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final SendPasswordResetEmailPort sendPasswordResetEmailPort;
    private final TransactionalOperator transactionalOperator;

    public RequestPasswordResetUseCaseImpl(
            IdentityRepository identityRepository,
            MembershipRepository membershipRepository,
            PasswordResetRepository passwordResetRepository,
            SendPasswordResetEmailPort sendPasswordResetEmailPort,
            TransactionalOperator transactionalOperator
    ) {
        this.identityRepository = Objects.requireNonNull(identityRepository, "identityRepository");
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "membershipRepository");
        this.passwordResetRepository = Objects.requireNonNull(
                passwordResetRepository,
                "passwordResetRepository"
        );
        this.sendPasswordResetEmailPort = Objects.requireNonNull(
                sendPasswordResetEmailPort,
                "sendPasswordResetEmailPort"
        );
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<Void> execute(RequestPasswordResetCommand command) {
        Objects.requireNonNull(command, "command");
        EmailAddress email = Objects.requireNonNull(command.email(), "email");
        Instant now = Instant.now();

        return identityRepository.findByEmail(email)
                .flatMap(identity -> resolveTenant(command.tenantId(), identity.tenantId())
                        .flatMap(tenantId -> membershipRepository
                                .findActiveByIdentityIdAndTenantId(identity.id(), tenantId)
                                .flatMap(membership -> createAndNotify(email, tenantId, identity.id(), now))
                                .then()))
                .then();
    }

    private Mono<TenantId> resolveTenant(TenantId requested, TenantId identityHomeTenant) {
        if (requested != null) {
            return Mono.just(requested);
        }
        return Mono.just(identityHomeTenant);
    }

    private Mono<Void> createAndNotify(
            EmailAddress email,
            TenantId tenantId,
            IdentityId identityId,
            Instant now
    ) {
        String rawToken = Sha256TokenHasher.generateRawToken();
        ResetTokenHash tokenHash = ResetTokenHash.ofHashedValue(Sha256TokenHasher.hash(rawToken));
        PasswordResetRequest request = PasswordResetRequest.create(
                tenantId,
                identityId,
                tokenHash,
                TokenExpiration.at(now.plus(TOKEN_TTL)),
                now
        );

        Mono<Void> persistAndSend = passwordResetRepository.save(request)
                .then(sendPasswordResetEmailPort.send(email, rawToken));

        return persistAndSend.as(transactionalOperator::transactional);
    }
}
