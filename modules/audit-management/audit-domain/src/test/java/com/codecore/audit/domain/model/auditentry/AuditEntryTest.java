package com.codecore.audit.domain.model.auditentry;

import com.codecore.audit.domain.valueobject.ActionCode;
import com.codecore.audit.domain.valueobject.AuditEntryId;
import com.codecore.audit.domain.valueobject.AuditOutcome;
import com.codecore.audit.domain.valueobject.MembershipId;
import com.codecore.audit.domain.valueobject.ResourceId;
import com.codecore.audit.domain.valueobject.ResourceType;
import com.codecore.audit.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEntryTest {

    private static final Instant NOW = Instant.parse("2026-07-13T12:00:00Z");

    @Test
    void shouldAppendWithDefaultSuccessOutcomeAndNullActor() {
        AuditEntryId id = AuditEntryId.generate();
        TenantId tenantId = TenantId.generate();
        ResourceId resourceId = ResourceId.of(UUID.randomUUID());

        AuditEntry entry = AuditEntry.append(
                id,
                tenantId,
                NOW,
                ActionCode.of("invitation.created"),
                null,
                ResourceType.of("invitation"),
                resourceId,
                null
        );

        assertThat(entry.id()).isEqualTo(id);
        assertThat(entry.tenantId()).isEqualTo(tenantId);
        assertThat(entry.occurredAt()).isEqualTo(NOW);
        assertThat(entry.actionCode().value()).isEqualTo("invitation.created");
        assertThat(entry.actorMembershipId()).isEmpty();
        assertThat(entry.resourceType().value()).isEqualTo("invitation");
        assertThat(entry.resourceId()).isEqualTo(resourceId);
        assertThat(entry.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(entry.createdAt()).isEqualTo(NOW);
    }

    @Test
    void shouldAppendWithActorAndFailureOutcome() {
        MembershipId actor = MembershipId.generate();

        AuditEntry entry = AuditEntry.append(
                AuditEntryId.generate(),
                TenantId.generate(),
                NOW,
                ActionCode.of("password_reset.completed"),
                actor,
                ResourceType.of("identity"),
                ResourceId.generate(),
                AuditOutcome.FAILURE
        );

        assertThat(entry.actorMembershipId()).contains(actor);
        assertThat(entry.outcome()).isEqualTo(AuditOutcome.FAILURE);
    }

    @Test
    void createShouldBeAliasOfAppend() {
        AuditEntryId id = AuditEntryId.generate();
        TenantId tenantId = TenantId.generate();
        ActionCode action = ActionCode.of("invitation.accepted");
        ResourceType type = ResourceType.of("invitation");
        ResourceId resourceId = ResourceId.generate();
        MembershipId actor = MembershipId.generate();

        AuditEntry viaAppend = AuditEntry.append(
                id, tenantId, NOW, action, actor, type, resourceId, AuditOutcome.SUCCESS
        );
        AuditEntry viaCreate = AuditEntry.create(
                id, tenantId, NOW, action, actor, type, resourceId, AuditOutcome.SUCCESS
        );

        assertThat(viaCreate.id()).isEqualTo(viaAppend.id());
        assertThat(viaCreate.tenantId()).isEqualTo(viaAppend.tenantId());
        assertThat(viaCreate.occurredAt()).isEqualTo(viaAppend.occurredAt());
        assertThat(viaCreate.actionCode()).isEqualTo(viaAppend.actionCode());
        assertThat(viaCreate.actorMembershipId()).isEqualTo(viaAppend.actorMembershipId());
        assertThat(viaCreate.resourceType()).isEqualTo(viaAppend.resourceType());
        assertThat(viaCreate.resourceId()).isEqualTo(viaAppend.resourceId());
        assertThat(viaCreate.outcome()).isEqualTo(viaAppend.outcome());
        assertThat(viaCreate.createdAt()).isEqualTo(viaAppend.createdAt());
    }

    @Test
    void shouldReconstitutePreservingCreatedAtDistinctFromOccurredAt() {
        Instant createdAt = NOW.minusSeconds(5);
        AuditEntryId id = AuditEntryId.generate();
        TenantId tenantId = TenantId.generate();

        AuditEntry entry = AuditEntry.reconstitute(
                id,
                tenantId,
                NOW,
                ActionCode.of("invitation.revoked"),
                MembershipId.generate(),
                ResourceType.of("invitation"),
                ResourceId.generate(),
                AuditOutcome.SUCCESS,
                createdAt
        );

        assertThat(entry.id()).isEqualTo(id);
        assertThat(entry.occurredAt()).isEqualTo(NOW);
        assertThat(entry.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void shouldKeepTenantIdImmutable() {
        AuditEntry entry = sampleEntry();
        TenantId original = entry.tenantId();

        assertThat(entry.tenantId()).isEqualTo(original);
    }

    @Test
    void shouldNeverExposeUpdateDeletePayloadOrMetricsInPublicApi() {
        assertThat(AuditEntry.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName)
                .doesNotContain(
                        "update",
                        "updateContent",
                        "delete",
                        "voidEntry",
                        "void",
                        "setPayload",
                        "payload",
                        "setMetrics",
                        "recordMetric",
                        "emitEvent",
                        "publish"
                );
    }

    private static AuditEntry sampleEntry() {
        return AuditEntry.append(
                AuditEntryId.generate(),
                TenantId.generate(),
                NOW,
                ActionCode.of("invitation.created"),
                null,
                ResourceType.of("invitation"),
                ResourceId.generate(),
                null
        );
    }
}
