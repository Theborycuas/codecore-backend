package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.infrastructure.persistence.entity.IamPasswordResetRequestEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataIamPasswordResetRequestRepository
        extends ReactiveCrudRepository<IamPasswordResetRequestEntity, UUID> {

    Mono<IamPasswordResetRequestEntity> findByTokenHash(String tokenHash);
}
