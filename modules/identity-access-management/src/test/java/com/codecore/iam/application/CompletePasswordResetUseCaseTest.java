package com.codecore.iam.application;

import com.codecore.iam.application.command.CompletePasswordResetCommand;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.PasswordHasher;
import com.codecore.iam.application.port.out.PasswordResetRepository;
import com.codecore.iam.domain.exception.InvalidTokenException;
import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.passwordreset.PasswordResetRequest;
import com.codecore.iam.domain.valueobject.CredentialId;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.PasswordResetStatus;
import com.codecore.iam.domain.valueobject.RawPassword;
import com.codecore.iam.domain.valueobject.ResetTokenHash;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TokenExpiration;
import com.codecore.iam.infrastructure.security.Sha256TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompletePasswordResetUseCaseTest {

    private static final String RAW_TOKEN = "aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899";
    private static final String NEW_PASSWORD = "NewSecure1!";
    private static final String NEW_HASH = "$2a$10$newhash";

    @Mock
    private PasswordResetRepository passwordResetRepository;

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private com.codecore.audit.contract.append.AuditAppendPort auditAppendPort;

    @Mock
    private TransactionalOperator transactionalOperator;

    private CompletePasswordResetUseCaseImpl useCase;

    private final TenantId tenantId = TenantId.generate();
    private final IdentityId identityId = IdentityId.generate();
    private final ResetTokenHash tokenHash =
            ResetTokenHash.ofHashedValue(Sha256TokenHasher.hash(RAW_TOKEN));

    @BeforeEach
    void setUp() {
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(auditAppendPort.append(any())).thenReturn(Mono.just(java.util.UUID.randomUUID()));
        useCase = new CompletePasswordResetUseCaseImpl(
                passwordResetRepository,
                identityRepository,
                passwordHasher,
                auditAppendPort,
                transactionalOperator
        );
    }

    @Test
    void shouldUpdatePasswordAndMarkRequestUsed() {
        Instant now = Instant.now();
        PasswordResetRequest request = PasswordResetRequest.create(
                tenantId,
                identityId,
                tokenHash,
                TokenExpiration.at(now.plusSeconds(3600)),
                now
        );
        Identity identity = identity(IdentityStatus.PASSWORD_RESET_REQUIRED);

        when(passwordResetRepository.findByTokenHash(tokenHash)).thenReturn(Mono.just(request));
        when(identityRepository.findById(identityId)).thenReturn(Mono.just(identity));
        when(passwordHasher.hash(NEW_PASSWORD)).thenReturn(NEW_HASH);
        when(identityRepository.save(any(Identity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(passwordResetRepository.save(any(PasswordResetRequest.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.execute(new CompletePasswordResetCommand(
                        tenantId,
                        tokenHash,
                        RawPassword.of(NEW_PASSWORD)
                )))
                .verifyComplete();

        ArgumentCaptor<Identity> identityCaptor = ArgumentCaptor.forClass(Identity.class);
        verify(identityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().status()).isEqualTo(IdentityStatus.ACTIVE);
        assertThat(identityCaptor.getValue().credential().orElseThrow().passwordHash().value())
                .isEqualTo(NEW_HASH);

        ArgumentCaptor<PasswordResetRequest> resetCaptor = ArgumentCaptor.forClass(PasswordResetRequest.class);
        verify(passwordResetRepository).save(resetCaptor.capture());
        assertThat(resetCaptor.getValue().status()).isEqualTo(PasswordResetStatus.USED);
        assertThat(resetCaptor.getValue().usedAt()).isNotNull();
    }

    @Test
    void shouldRejectUnknownToken() {
        when(passwordResetRepository.findByTokenHash(tokenHash)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(new CompletePasswordResetCommand(
                        tenantId,
                        tokenHash,
                        RawPassword.of(NEW_PASSWORD)
                )))
                .expectError(InvalidTokenException.class)
                .verify();
    }

    @Test
    void shouldRejectTenantMismatch() {
        Instant now = Instant.now();
        PasswordResetRequest request = PasswordResetRequest.create(
                tenantId,
                identityId,
                tokenHash,
                TokenExpiration.at(now.plusSeconds(3600)),
                now
        );
        when(passwordResetRepository.findByTokenHash(tokenHash)).thenReturn(Mono.just(request));

        StepVerifier.create(useCase.execute(new CompletePasswordResetCommand(
                        TenantId.generate(),
                        tokenHash,
                        RawPassword.of(NEW_PASSWORD)
                )))
                .expectError(InvalidTokenException.class)
                .verify();
    }

    private Identity identity(IdentityStatus status) {
        Instant now = Instant.now();
        return new Identity(
                identityId,
                tenantId,
                EmailAddress.of("reset@codecore.local"),
                status,
                new Credential(
                        new CredentialId(identityId.value()),
                        PasswordHash.ofHashedValue("$2a$10$oldhash"),
                        now,
                        null,
                        true,
                        0L
                ),
                null,
                now,
                now,
                0L
        );
    }
}
