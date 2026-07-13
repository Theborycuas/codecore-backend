package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.PasswordResetRepository;
import com.codecore.iam.domain.model.passwordreset.PasswordResetRequest;
import com.codecore.iam.domain.valueobject.PasswordResetRequestId;
import com.codecore.iam.domain.valueobject.ResetTokenHash;
import com.codecore.iam.infrastructure.persistence.entity.IamPasswordResetRequestEntity;
import com.codecore.iam.infrastructure.persistence.mapper.IamPasswordResetRequestMapper;
import reactor.core.publisher.Mono;

/**
 * Hexagonal adapter: implements {@link PasswordResetRepository} using R2DBC.
 * Wired as a {@code @Bean} in {@code IamModuleConfiguration} (not component-scanned)
 * so slice/IT configs that import the module get the port without extra {@code @Import}s.
 */
public class R2dbcPasswordResetRepository implements PasswordResetRepository {

    private final SpringDataIamPasswordResetRequestRepository springDataRepository;
    private final IamPasswordResetRequestMapper mapper;

    public R2dbcPasswordResetRepository(
            SpringDataIamPasswordResetRequestRepository springDataRepository,
            IamPasswordResetRequestMapper mapper
    ) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Mono<PasswordResetRequest> save(PasswordResetRequest request) {
        return springDataRepository.findById(request.id().value())
                .flatMap(existing -> {
                    IamPasswordResetRequestEntity entity = mapper.toEntity(request, false);
                    entity.setVersion(existing.getVersion());
                    return springDataRepository.save(entity);
                })
                .switchIfEmpty(Mono.defer(() -> springDataRepository.save(
                        mapper.toEntity(request, true))))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<PasswordResetRequest> findById(PasswordResetRequestId id) {
        return springDataRepository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public Mono<PasswordResetRequest> findByTokenHash(ResetTokenHash tokenHash) {
        return springDataRepository.findByTokenHash(tokenHash.value())
                .map(mapper::toDomain);
    }
}
