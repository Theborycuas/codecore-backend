package com.codecore.audit.application.admin;

import com.codecore.audit.application.port.out.AuditAdminQueryRepository;
import com.codecore.audit.application.port.out.AuditEntryQueryPort;
import com.codecore.audit.application.port.out.AuditEntryRepository;
import com.codecore.audit.application.port.out.TenantContextAccessor;
import com.codecore.audit.contract.append.AuditAppendPort;
import com.codecore.audit.domain.exception.ActorMembershipNotFoundException;
import com.codecore.audit.domain.exception.InvalidDomainValueException;
import com.codecore.audit.domain.model.auditentry.AuditEntry;
import com.codecore.audit.domain.valueobject.AuditOutcome;
import com.codecore.iam.contract.reference.IamMembershipReferencePort;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAdministrationUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-07-13T15:00:00Z");

    @Mock
    private TenantContextAccessor tenantContextAccessor;
    @Mock
    private AuditAdminQueryRepository auditAdminQueryRepository;
    @Mock
    private AuditEntryRepository auditEntryRepository;
    @Mock
    private AuditEntryQueryPort auditEntryQueryPort;
    @Mock
    private IamMembershipReferencePort iamMembershipReferencePort;
    @Mock
    private TransactionalOperator transactionalOperator;

    private AuditAdministrationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        lenient().when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        useCase = new AuditAdministrationUseCaseImpl(
                tenantContextAccessor,
                auditAdminQueryRepository,
                auditEntryRepository,
                auditEntryQueryPort,
                iamMembershipReferencePort,
                transactionalOperator
        );
    }

    @Test
    void appendShouldPersistWithoutActorValidationWhenActorNull() {
        UUID tenantId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(auditEntryRepository.save(any(AuditEntry.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        AuditAppendPort.AppendAuditCommand cmd = new AuditAppendPort.AppendAuditCommand(
                tenantId,
                "invitation.created",
                null,
                "invitation",
                resourceId,
                null,
                NOW
        );

        StepVerifier.create(useCase.append(cmd))
                .assertNext(id -> assertThat(id).isNotNull())
                .verifyComplete();

        verify(iamMembershipReferencePort, never()).existsActiveByIdAndTenant(any(), any());
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditEntryRepository).save(captor.capture());
        assertThat(captor.getValue().outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(captor.getValue().actorMembershipId()).isEmpty();
    }

    @Test
    void appendShouldValidateActiveActorWhenPresent() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(iamMembershipReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(true));
        when(auditEntryRepository.save(any(AuditEntry.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        AuditAppendPort.AppendAuditCommand cmd = new AuditAppendPort.AppendAuditCommand(
                tenantId,
                "invitation.accepted",
                actorId,
                "invitation",
                resourceId,
                "SUCCESS",
                NOW
        );

        StepVerifier.create(useCase.append(cmd))
                .assertNext(id -> assertThat(id).isNotNull())
                .verifyComplete();

        verify(iamMembershipReferencePort).existsActiveByIdAndTenant(any(), any());
    }

    @Test
    void appendShouldFailWhenActorNotActive() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(iamMembershipReferencePort.existsActiveByIdAndTenant(any(), any())).thenReturn(Mono.just(false));

        AuditAppendPort.AppendAuditCommand cmd = new AuditAppendPort.AppendAuditCommand(
                tenantId,
                "invitation.created",
                actorId,
                "invitation",
                UUID.randomUUID(),
                null,
                NOW
        );

        StepVerifier.create(useCase.append(cmd))
                .expectError(ActorMembershipNotFoundException.class)
                .verify();

        verify(auditEntryRepository, never()).save(any());
    }

    @Test
    void appendShouldRejectInvalidOutcome() {
        AuditAppendPort.AppendAuditCommand cmd = new AuditAppendPort.AppendAuditCommand(
                UUID.randomUUID(),
                "invitation.created",
                null,
                "invitation",
                UUID.randomUUID(),
                "MAYBE",
                NOW
        );

        StepVerifier.create(useCase.append(cmd))
                .expectError(InvalidDomainValueException.class)
                .verify();
    }

    @Test
    void appendShouldRejectBlankActionCode() {
        AuditAppendPort.AppendAuditCommand cmd = new AuditAppendPort.AppendAuditCommand(
                UUID.randomUUID(),
                "  ",
                null,
                "invitation",
                UUID.randomUUID(),
                null,
                NOW
        );

        StepVerifier.create(useCase.append(cmd))
                .expectError(InvalidDomainValueException.class)
                .verify();
    }
}
