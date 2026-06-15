package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.infrastructure.persistence.entity.IamPermissionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataIamPermissionRepository extends ReactiveCrudRepository<IamPermissionEntity, UUID> {

    Mono<IamPermissionEntity> findByCode(String code);

    Mono<Boolean> existsByCode(String code);
}
