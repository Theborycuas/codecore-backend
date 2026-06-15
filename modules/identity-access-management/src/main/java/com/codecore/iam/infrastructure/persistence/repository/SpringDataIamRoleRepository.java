package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.infrastructure.persistence.entity.IamRoleEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataIamRoleRepository extends ReactiveCrudRepository<IamRoleEntity, UUID> {

    Mono<IamRoleEntity> findByTenantIdAndCode(UUID tenantId, String code);

    Mono<Boolean> existsByTenantIdAndCode(UUID tenantId, String code);
}
