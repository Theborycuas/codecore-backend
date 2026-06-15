package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.infrastructure.persistence.entity.IamUserEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Spring Data R2DBC repository for {@code iam.iam_user} (email-first lookups).
 */
public interface SpringDataIamUserRepository extends ReactiveCrudRepository<IamUserEntity, UUID> {

    Mono<IamUserEntity> findFirstByNormalizedEmailOrderByCreatedAtAsc(String normalizedEmail);

    Mono<Boolean> existsByNormalizedEmail(String normalizedEmail);

    Mono<IamUserEntity> findByTenantIdAndNormalizedEmail(UUID tenantId, String normalizedEmail);

    Mono<Boolean> existsByTenantIdAndNormalizedEmail(UUID tenantId, String normalizedEmail);

    Mono<IamUserEntity> findByTenantIdAndId(UUID tenantId, UUID id);
}
