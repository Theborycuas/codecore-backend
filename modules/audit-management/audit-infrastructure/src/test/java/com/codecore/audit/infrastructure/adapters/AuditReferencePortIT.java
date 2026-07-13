package com.codecore.audit.infrastructure.adapters;

import com.codecore.audit.application.port.out.AuditEntryRepository;
import com.codecore.audit.contract.reference.AuditReferencePort;
import com.codecore.audit.domain.model.auditentry.AuditEntry;
import com.codecore.audit.domain.valueobject.ActionCode;
import com.codecore.audit.domain.valueobject.AuditEntryId;
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

@DataR2dbcTest
@Import({
        AuditPersistenceTestConfiguration.class,
        R2dbcAuditReferenceAdapter.class
})
class AuditReferencePortIT extends AbstractPostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-13T21:00:00Z");

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    @Autowired
    private AuditReferencePort auditReferencePort;

    @Test
    void shouldReturnTrueWhenEntryExistsInTenant() {
        AuditEntryId id = AuditEntryId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(auditEntryRepository.save(sample(id, tenantId)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(auditReferencePort.existsByIdAndTenant(id, tenantId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldReturnFalseForWrongTenantOrUnknownId() {
        AuditEntryId id = AuditEntryId.generate();
        TenantId tenantId = TenantId.generate();

        StepVerifier.create(auditEntryRepository.save(sample(id, tenantId)))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(auditReferencePort.existsByIdAndTenant(id, TenantId.generate()))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(auditReferencePort.existsByIdAndTenant(AuditEntryId.generate(), tenantId))
                .expectNext(false)
                .verifyComplete();
    }

    private static AuditEntry sample(AuditEntryId id, TenantId tenantId) {
        return AuditEntry.append(
                id,
                tenantId,
                NOW,
                ActionCode.of("invitation.created"),
                null,
                ResourceType.of("invitation"),
                ResourceId.of(UUID.randomUUID()),
                null
        );
    }
}
