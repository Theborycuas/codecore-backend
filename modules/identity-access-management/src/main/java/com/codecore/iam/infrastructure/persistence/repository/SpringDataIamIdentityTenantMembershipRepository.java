package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.infrastructure.persistence.entity.IamIdentityTenantMembershipEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataIamIdentityTenantMembershipRepository
        extends ReactiveCrudRepository<IamIdentityTenantMembershipEntity, UUID> {

    Mono<Boolean> existsByIdentityIdAndTenantId(UUID identityId, UUID tenantId);

    Flux<IamIdentityTenantMembershipEntity> findByIdentityId(UUID identityId);

    Flux<IamIdentityTenantMembershipEntity> findByTenantId(UUID tenantId);

    Mono<IamIdentityTenantMembershipEntity> findByIdentityIdAndTenantIdAndStatus(
            UUID identityId,
            UUID tenantId,
            String status
    );

    Mono<IamIdentityTenantMembershipEntity> findByIdentityIdAndTenantId(UUID identityId, UUID tenantId);

    Mono<IamIdentityTenantMembershipEntity> findByMembershipIdAndTenantId(UUID membershipId, UUID tenantId);
}
