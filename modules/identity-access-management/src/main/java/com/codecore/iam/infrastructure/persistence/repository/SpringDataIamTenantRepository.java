package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.infrastructure.persistence.entity.IamTenantEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataIamTenantRepository extends ReactiveCrudRepository<IamTenantEntity, UUID> {

    Mono<Boolean> existsByName(String name);
}
