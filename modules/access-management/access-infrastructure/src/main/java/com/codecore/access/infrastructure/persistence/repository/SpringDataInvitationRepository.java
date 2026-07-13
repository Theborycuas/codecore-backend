package com.codecore.access.infrastructure.persistence.repository;

import com.codecore.access.infrastructure.persistence.entity.InvitationEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataInvitationRepository extends ReactiveCrudRepository<InvitationEntity, UUID> {

    Mono<InvitationEntity> findByInvitationIdAndTenantId(UUID invitationId, UUID tenantId);

    Mono<InvitationEntity> findByTokenHash(String tokenHash);

    Mono<Boolean> existsByInvitationIdAndTenantId(UUID invitationId, UUID tenantId);

    Mono<Boolean> existsByTenantIdAndInvitedEmailAndStatus(UUID tenantId, String invitedEmail, String status);
}
