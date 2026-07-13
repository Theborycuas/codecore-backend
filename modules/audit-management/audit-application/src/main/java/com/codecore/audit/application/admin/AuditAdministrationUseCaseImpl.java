package com.codecore.audit.application.admin;

import com.codecore.audit.application.dto.AdminAuditView;
import com.codecore.audit.application.dto.PagedResult;
import com.codecore.audit.application.port.in.AppendAuditUseCase;
import com.codecore.audit.application.port.in.GetAuditEntryUseCase;
import com.codecore.audit.application.port.in.ListAuditEntriesUseCase;
import com.codecore.audit.application.port.out.AuditAdminQueryRepository;
import com.codecore.audit.application.port.out.AuditEntryQueryPort;
import com.codecore.audit.application.port.out.AuditEntryRepository;
import com.codecore.audit.application.port.out.TenantContextAccessor;
import com.codecore.audit.application.query.AuditListQuery;
import com.codecore.audit.application.query.PageQuery;
import com.codecore.audit.contract.append.AuditAppendPort;
import com.codecore.audit.domain.exception.ActorMembershipNotFoundException;
import com.codecore.audit.domain.exception.AuditEntryNotFoundException;
import com.codecore.audit.domain.exception.InvalidDomainValueException;
import com.codecore.audit.domain.model.auditentry.AuditEntry;
import com.codecore.audit.domain.valueobject.ActionCode;
import com.codecore.audit.domain.valueobject.AuditEntryId;
import com.codecore.audit.domain.valueobject.AuditOutcome;
import com.codecore.audit.domain.valueobject.MembershipId;
import com.codecore.audit.domain.valueobject.ResourceId;
import com.codecore.audit.domain.valueobject.ResourceType;
import com.codecore.audit.domain.valueobject.TenantId;
import com.codecore.iam.contract.reference.IamMembershipReferencePort;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Audit administration + append use cases (PASO 24.6) — write-time validation of optional actor
 * via {@link IamMembershipReferencePort} (ADR-013 / ADR-020). Append-only: no update/void/delete.
 */
public final class AuditAdministrationUseCaseImpl
        implements ListAuditEntriesUseCase,
        GetAuditEntryUseCase,
        AppendAuditUseCase {

    private final TenantContextAccessor tenantContextAccessor;
    private final AuditAdminQueryRepository auditAdminQueryRepository;
    private final AuditEntryRepository auditEntryRepository;
    private final AuditEntryQueryPort auditEntryQueryPort;
    private final IamMembershipReferencePort iamMembershipReferencePort;
    private final TransactionalOperator transactionalOperator;

    public AuditAdministrationUseCaseImpl(
            TenantContextAccessor tenantContextAccessor,
            AuditAdminQueryRepository auditAdminQueryRepository,
            AuditEntryRepository auditEntryRepository,
            AuditEntryQueryPort auditEntryQueryPort,
            IamMembershipReferencePort iamMembershipReferencePort,
            TransactionalOperator transactionalOperator
    ) {
        this.tenantContextAccessor = Objects.requireNonNull(tenantContextAccessor, "tenantContextAccessor");
        this.auditAdminQueryRepository = Objects.requireNonNull(
                auditAdminQueryRepository,
                "auditAdminQueryRepository"
        );
        this.auditEntryRepository = Objects.requireNonNull(auditEntryRepository, "auditEntryRepository");
        this.auditEntryQueryPort = Objects.requireNonNull(auditEntryQueryPort, "auditEntryQueryPort");
        this.iamMembershipReferencePort = Objects.requireNonNull(
                iamMembershipReferencePort,
                "iamMembershipReferencePort"
        );
        this.transactionalOperator = Objects.requireNonNull(transactionalOperator, "transactionalOperator");
    }

    @Override
    public Mono<PagedResult<AdminAuditView>> execute(AuditListQuery filter, PageQuery pageQuery) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> auditAdminQueryRepository.countByTenantId(tenantId, filter)
                        .flatMap(total -> auditAdminQueryRepository
                                .findByTenantId(tenantId, filter, pageQuery)
                                .collectList()
                                .map(content -> PagedResult.of(
                                        content,
                                        pageQuery.page(),
                                        pageQuery.size(),
                                        total
                                ))));
    }

    @Override
    public Mono<AdminAuditView> execute(AuditEntryId auditEntryId) {
        return tenantContextAccessor.currentTenantId()
                .flatMap(tenantId -> loadInTenant(tenantId, auditEntryId).map(this::toView));
    }

    @Override
    public Mono<UUID> append(AuditAppendPort.AppendAuditCommand cmd) {
        Objects.requireNonNull(cmd, "cmd");
        return Mono.defer(() -> {
                    if (cmd.tenantId() == null) {
                        return Mono.error(new InvalidDomainValueException("tenantId is required"));
                    }
                    if (cmd.occurredAt() == null) {
                        return Mono.error(new InvalidDomainValueException("occurredAt is required"));
                    }
                    if (cmd.resourceId() == null) {
                        return Mono.error(new InvalidDomainValueException("resourceId is required"));
                    }

                    TenantId tenantId = TenantId.of(cmd.tenantId());
                    ActionCode actionCode = ActionCode.of(cmd.actionCode());
                    ResourceType resourceType = ResourceType.of(cmd.resourceType());
                    ResourceId resourceId = ResourceId.of(cmd.resourceId());
                    MembershipId actorOrNull = cmd.actorMembershipIdOrNull() == null
                            ? null
                            : MembershipId.of(cmd.actorMembershipIdOrNull());
                    AuditOutcome outcome = parseOutcome(cmd.outcomeOrNull());
                    Instant occurredAt = cmd.occurredAt();

                    Mono<Void> actorValidation = actorOrNull == null
                            ? Mono.empty()
                            : validateActorActive(tenantId, actorOrNull);

                    return actorValidation.then(Mono.defer(() -> {
                        AuditEntry entry = AuditEntry.append(
                                AuditEntryId.generate(),
                                tenantId,
                                occurredAt,
                                actionCode,
                                actorOrNull,
                                resourceType,
                                resourceId,
                                outcome
                        );
                        return auditEntryRepository.save(entry).map(saved -> saved.id().value());
                    }));
                })
                .as(transactionalOperator::transactional);
    }

    private Mono<Void> validateActorActive(TenantId tenantId, MembershipId actor) {
        return iamMembershipReferencePort.existsActiveByIdAndTenant(
                        new com.codecore.iam.domain.valueobject.MembershipId(actor.value()),
                        new com.codecore.iam.domain.valueobject.TenantId(tenantId.value())
                )
                .flatMap(exists -> exists
                        ? Mono.<Void>empty()
                        : Mono.error(new ActorMembershipNotFoundException(
                                "Actor membership not found, not in tenant, or not ACTIVE")));
    }

    private static AuditOutcome parseOutcome(String outcomeOrNull) {
        if (outcomeOrNull == null || outcomeOrNull.isBlank()) {
            return AuditOutcome.SUCCESS;
        }
        try {
            return AuditOutcome.valueOf(outcomeOrNull.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidDomainValueException("outcome must be SUCCESS or FAILURE");
        }
    }

    private Mono<AuditEntry> loadInTenant(TenantId tenantId, AuditEntryId auditEntryId) {
        return auditEntryQueryPort.findByIdAndTenantId(auditEntryId, tenantId)
                .switchIfEmpty(Mono.error(new AuditEntryNotFoundException(
                        "Audit entry not found in tenant context")));
    }

    private AdminAuditView toView(AuditEntry entry) {
        return new AdminAuditView(
                entry.id(),
                entry.tenantId(),
                entry.occurredAt(),
                entry.actionCode(),
                entry.actorMembershipId().orElse(null),
                entry.resourceType(),
                entry.resourceId(),
                entry.outcome(),
                entry.createdAt()
        );
    }
}
