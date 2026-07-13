package com.codecore.iam.application;

import com.codecore.audit.contract.append.AuditAppendPort;
import com.codecore.iam.application.command.CompletePasswordResetCommand;
import com.codecore.iam.application.port.in.CompletePasswordResetUseCase;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.application.port.out.PasswordResetRepository;
import com.codecore.iam.domain.exception.AuthenticationNotPermittedException;
import com.codecore.iam.domain.exception.InvalidTokenException;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.passwordreset.PasswordResetRequest;
import com.codecore.iam.domain.valueobject.PasswordHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

/**
 * Completes password recovery using a hashed reset token.
 * Best-effort audit append via {@link AuditAppendPort} (FASE 24).
 */
public final class CompletePasswordResetUseCaseImpl implements CompletePasswordResetUseCase {

    private static final Logger log = LoggerFactory.getLogger(CompletePasswordResetUseCaseImpl.class);

    private final PasswordResetRepository passwordResetRepository;
    private final IdentityRepository identityRepository;
    private final PasswordHasher passwordHasher;
    private final AuditAppendPort auditAppendPort;
    private final TransactionalOperator transactionalOperator;

    public CompletePasswordResetUseCaseImpl(
            PasswordResetRepository passwordResetRepository,
            IdentityRepository identityRepository,
            PasswordHasher passwordHasher,
            AuditAppendPort auditAppendPort,
            TransactionalOperator transactionalOperator
    ) {
        this.passwordResetRepository = Objects.requireNonNull(
                passwordResetRepository,
                "passwordResetRepository"
        );
        this.identityRepository = Objects.requireNonNull(identityRepository, "identityRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.auditAppendPort = Objects.requireNonNull(auditAppendPort, "auditAppendPort");
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<Void> execute(CompletePasswordResetCommand command) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(command.resetTokenHash(), "resetTokenHash");
        Objects.requireNonNull(command.newPassword(), "newPassword");
        Instant now = Instant.now();

        Mono<Void> flow = passwordResetRepository.findByTokenHash(command.resetTokenHash())
                .switchIfEmpty(Mono.error(new InvalidTokenException("Password reset token is invalid")))
                .flatMap(request -> validateTenant(request, command).thenReturn(request))
                .flatMap(request -> {
                    request.validateResetEligibility(now);
                    return identityRepository.findById(request.identityId())
                            .switchIfEmpty(Mono.error(new AuthenticationNotPermittedException(
                                    "Identity not found for password reset")))
                            .flatMap(identity -> applyPassword(identity, request, command, now));
                });

        return flow.as(transactionalOperator::transactional);
    }

    private Mono<Void> validateTenant(PasswordResetRequest request, CompletePasswordResetCommand command) {
        if (command.tenantId() != null && !command.tenantId().equals(request.tenantId())) {
            return Mono.error(new InvalidTokenException("Password reset token is invalid"));
        }
        return Mono.empty();
    }

    private Mono<Void> applyPassword(
            Identity identity,
            PasswordResetRequest request,
            CompletePasswordResetCommand command,
            Instant now
    ) {
        String hashed = passwordHasher.hash(command.newPassword().value());
        identity.changePassword(PasswordHash.ofHashedValue(hashed), now);
        request.markUsed(now);
        return identityRepository.save(identity)
                .then(passwordResetRepository.save(request))
                .flatMap(saved -> appendAuditBestEffort(request, now))
                .then();
    }

    private Mono<Void> appendAuditBestEffort(PasswordResetRequest request, Instant now) {
        return auditAppendPort.append(new AuditAppendPort.AppendAuditCommand(
                        request.tenantId().value(),
                        "password_reset.completed",
                        null,
                        "identity",
                        request.identityId().value(),
                        "SUCCESS",
                        now
                ))
                .onErrorResume(ex -> {
                    log.warn("Best-effort audit append failed for password_reset.completed: {}", ex.toString());
                    return Mono.empty();
                })
                .then();
    }
}
