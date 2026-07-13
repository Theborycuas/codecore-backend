package com.codecore.audit.infrastructure.persistence.repository;

import com.codecore.audit.application.port.out.AuditEntryQueryPort;
import com.codecore.audit.application.port.out.AuditEntryRepository;
import com.codecore.audit.domain.model.auditentry.AuditEntry;
import com.codecore.audit.domain.valueobject.ActionCode;
import com.codecore.audit.domain.valueobject.AuditEntryId;
import com.codecore.audit.domain.valueobject.AuditOutcome;
import com.codecore.audit.domain.valueobject.MembershipId;
import com.codecore.audit.domain.valueobject.ResourceId;
import com.codecore.audit.domain.valueobject.ResourceType;
import com.codecore.audit.domain.valueobject.TenantId;
import com.codecore.audit.testsupport.AbstractPostgresIntegrationTest;
import com.codecore.audit.testsupport.AuditPersistenceTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(AuditPersistenceTestConfiguration.class)
class R2dbcAuditEntryRepositoryIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-13T19:00:00Z");

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    @Autowired
    private AuditEntryQueryPort auditEntryQueryPort;

    @Test
    void shouldPersistAndFindById() {
        AuditEntryId id = AuditEntryId.generate();
        TenantId tenantId = TenantId.generate();
        AuditEntry entry = sampleEntry(id, tenantId, null);

        StepVerifier.create(auditEntryRepository.save(entry))
                .assertNext(saved -> {
                    assertThat(saved.id()).isEqualTo(id);
                    assertThat(saved.tenantId()).isEqualTo(tenantId);
                    assertThat(saved.outcome()).isEqualTo(AuditOutcome.SUCCESS);
                    assertThat(saved.actionCode().value()).isEqualTo("invitation.created");
                })
                .verifyComplete();

        StepVerifier.create(auditEntryRepository.findById(id))
                .assertNext(found -> assertThat(found.id()).isEqualTo(id))
                .verifyComplete();
    }

    @Test
    void shouldPersistWithActorAndFailureOutcome() {
        AuditEntryId id = AuditEntryId.generate();
        TenantId tenantId = TenantId.generate();
        MembershipId actor = MembershipId.generate();
        AuditEntry entry = AuditEntry.append(
                id,
                tenantId,
                NOW,
                ActionCode.of("password_reset.completed"),
                actor,
                ResourceType.of("identity"),
                ResourceId.generate(),
                AuditOutcome.FAILURE
        );

        StepVerifier.create(auditEntryRepository.save(entry))
                .assertNext(saved -> {
                    assertThat(saved.actorMembershipId()).contains(actor);
                    assertThat(saved.outcome()).isEqualTo(AuditOutcome.FAILURE);
                })
                .verifyComplete();
    }

    @Test
    void shouldReportExistsByIdAndTenant() {
        AuditEntryId id = AuditEntryId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(auditEntryRepository.save(sampleEntry(id, tenantId, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(auditEntryRepository.existsById(id))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(auditEntryRepository.existsByIdAndTenantId(id, tenantId))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(auditEntryRepository.existsByIdAndTenantId(id, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldIsolateCrossTenantReads() {
        AuditEntryId id = AuditEntryId.generate();
        TenantId tenantA = TenantId.generate();

        StepVerifier.create(auditEntryRepository.save(sampleEntry(id, tenantA, null)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(auditEntryQueryPort.findByIdAndTenantId(id, TenantId.generate()))
                .verifyComplete();
    }

    private static AuditEntry sampleEntry(AuditEntryId id, TenantId tenantId, MembershipId actor) {
        return AuditEntry.append(
                id,
                tenantId,
                NOW,
                ActionCode.of("invitation.created"),
                actor,
                ResourceType.of("invitation"),
                ResourceId.of(UUID.randomUUID()),
                null
        );
    }
}
