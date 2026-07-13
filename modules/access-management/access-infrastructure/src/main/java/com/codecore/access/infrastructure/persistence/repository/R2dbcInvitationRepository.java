package com.codecore.access.infrastructure.persistence.repository;

import com.codecore.access.application.port.out.InvitationQueryPort;
import com.codecore.access.application.port.out.InvitationRepository;
import com.codecore.access.domain.model.invitation.Invitation;
import com.codecore.access.domain.valueobject.EmailAddress;
import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.domain.valueobject.InvitationStatus;
import com.codecore.access.domain.valueobject.InvitationTokenHash;
import com.codecore.access.domain.valueobject.TenantId;
import com.codecore.access.infrastructure.persistence.mapper.InvitationMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Hexagonal adapter: implements outbound Invitation persistence ports using R2DBC (ADR-019).
 */
@Repository
public class R2dbcInvitationRepository implements InvitationRepository, InvitationQueryPort {

    private final SpringDataInvitationRepository springDataInvitationRepository;
    private final InvitationMapper invitationMapper;

    public R2dbcInvitationRepository(
            SpringDataInvitationRepository springDataInvitationRepository,
            InvitationMapper invitationMapper
    ) {
        this.springDataInvitationRepository = springDataInvitationRepository;
        this.invitationMapper = invitationMapper;
    }

    @Override
    public Mono<Invitation> save(Invitation invitation) {
        return springDataInvitationRepository
                .existsById(invitation.id().value())
                .flatMap(exists -> springDataInvitationRepository.save(
                        invitationMapper.toEntity(invitation, !exists)))
                .map(invitationMapper::toDomain);
    }

    @Override
    public Mono<Invitation> findById(InvitationId id) {
        return springDataInvitationRepository.findById(id.value())
                .map(invitationMapper::toDomain);
    }

    @Override
    public Mono<Invitation> findByIdAndTenantId(InvitationId id, TenantId tenantId) {
        return springDataInvitationRepository.findByInvitationIdAndTenantId(id.value(), tenantId.value())
                .map(invitationMapper::toDomain);
    }

    @Override
    public Mono<Invitation> findByTokenHash(InvitationTokenHash tokenHash) {
        return springDataInvitationRepository.findByTokenHash(tokenHash.value())
                .map(invitationMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsPendingByEmailAndTenant(EmailAddress email, TenantId tenantId) {
        return springDataInvitationRepository.existsByTenantIdAndInvitedEmailAndStatus(
                tenantId.value(),
                email.value(),
                InvitationStatus.PENDING.name()
        );
    }
}
