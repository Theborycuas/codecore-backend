package com.codecore.iam.application;

import com.codecore.iam.application.command.RequestPasswordResetCommand;
import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.application.port.out.PasswordResetRepository;
import com.codecore.iam.application.port.out.SendPasswordResetEmailPort;
import com.codecore.iam.domain.model.identity.Credential;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.model.passwordreset.PasswordResetRequest;
import com.codecore.iam.domain.valueobject.CredentialId;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.IdentityStatus;
import com.codecore.iam.domain.valueobject.PasswordHash;
import com.codecore.iam.domain.valueobject.TenantId;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestPasswordResetUseCaseTest {

    private static final String EMAIL = "reset@codecore.local";

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private PasswordResetRepository passwordResetRepository;

    @Mock
    private SendPasswordResetEmailPort sendPasswordResetEmailPort;

    @Mock
    private com.codecore.audit.contract.append.AuditAppendPort auditAppendPort;

    @Mock
    private TransactionalOperator transactionalOperator;

    private RequestPasswordResetUseCaseImpl useCase;

    private final TenantId tenantId = TenantId.generate();
    private final IdentityId identityId = IdentityId.generate();

    @BeforeEach
    void setUp() {
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(auditAppendPort.append(any())).thenReturn(Mono.just(java.util.UUID.randomUUID()));
        useCase = new RequestPasswordResetUseCaseImpl(
                identityRepository,
                membershipRepository,
                passwordResetRepository,
                sendPasswordResetEmailPort,
                auditAppendPort,
                transactionalOperator
        );
    }

    @Test
    void shouldCreateResetRequestAndSendEmailWhenIdentityAndActiveMembershipExist() {
        Identity identity = identity(tenantId, EMAIL);
        when(identityRepository.findByEmail(EmailAddress.of(EMAIL))).thenReturn(Mono.just(identity));
        when(membershipRepository.findActiveByIdentityIdAndTenantId(identityId, tenantId))
                .thenReturn(Mono.just(IdentityTenantMembership.create(identityId, tenantId, Instant.now())));
        when(passwordResetRepository.save(any(PasswordResetRequest.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(sendPasswordResetEmailPort.send(eq(EmailAddress.of(EMAIL)), any(String.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(new RequestPasswordResetCommand(tenantId, EmailAddress.of(EMAIL))))
                .verifyComplete();

        ArgumentCaptor<PasswordResetRequest> requestCaptor = ArgumentCaptor.forClass(PasswordResetRequest.class);
        verify(passwordResetRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().identityId()).isEqualTo(identityId);
        assertThat(requestCaptor.getValue().tenantId()).isEqualTo(tenantId);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(sendPasswordResetEmailPort).send(eq(EmailAddress.of(EMAIL)), tokenCaptor.capture());
        assertThat(tokenCaptor.getValue()).isNotBlank();
    }

    @Test
    void shouldSucceedSilentlyWhenEmailUnknown() {
        when(identityRepository.findByEmail(EmailAddress.of(EMAIL))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(new RequestPasswordResetCommand(tenantId, EmailAddress.of(EMAIL))))
                .verifyComplete();

        verify(passwordResetRepository, never()).save(any());
        verify(sendPasswordResetEmailPort, never()).send(any(), any());
    }

    @Test
    void shouldSucceedSilentlyWhenNoActiveMembership() {
        Identity identity = identity(tenantId, EMAIL);
        when(identityRepository.findByEmail(EmailAddress.of(EMAIL))).thenReturn(Mono.just(identity));
        when(membershipRepository.findActiveByIdentityIdAndTenantId(identityId, tenantId))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(new RequestPasswordResetCommand(tenantId, EmailAddress.of(EMAIL))))
                .verifyComplete();

        verify(passwordResetRepository, never()).save(any());
        verify(sendPasswordResetEmailPort, never()).send(any(), any());
    }

    private Identity identity(TenantId tenant, String email) {
        Instant now = Instant.now();
        return new Identity(
                identityId,
                tenant,
                EmailAddress.of(email),
                IdentityStatus.ACTIVE,
                new Credential(
                        new CredentialId(identityId.value()),
                        PasswordHash.ofHashedValue("$2a$10$storedhash"),
                        now,
                        null,
                        false,
                        0L
                ),
                null,
                now,
                now,
                0L
        );
    }
}
