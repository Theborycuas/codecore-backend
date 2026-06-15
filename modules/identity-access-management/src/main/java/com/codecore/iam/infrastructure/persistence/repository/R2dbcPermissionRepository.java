package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.PermissionRepository;
import com.codecore.iam.domain.model.permission.Permission;
import com.codecore.iam.domain.valueobject.PermissionCode;
import com.codecore.iam.domain.valueobject.PermissionId;
import com.codecore.iam.infrastructure.persistence.mapper.IamPermissionMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Hexagonal adapter: implements the application outbound port using R2DBC.
 */
@Repository
public class R2dbcPermissionRepository implements PermissionRepository {

    private final SpringDataIamPermissionRepository springDataIamPermissionRepository;
    private final IamPermissionMapper iamPermissionMapper;

    public R2dbcPermissionRepository(
            SpringDataIamPermissionRepository springDataIamPermissionRepository,
            IamPermissionMapper iamPermissionMapper
    ) {
        this.springDataIamPermissionRepository = springDataIamPermissionRepository;
        this.iamPermissionMapper = iamPermissionMapper;
    }

    @Override
    public Mono<Permission> save(Permission permission) {
        return springDataIamPermissionRepository
                .existsById(permission.id().value())
                .flatMap(exists -> springDataIamPermissionRepository.save(
                        iamPermissionMapper.toEntity(permission, !exists)))
                .map(iamPermissionMapper::toDomain);
    }

    @Override
    public Mono<Permission> findById(PermissionId permissionId) {
        return springDataIamPermissionRepository.findById(permissionId.value())
                .map(iamPermissionMapper::toDomain);
    }

    @Override
    public Mono<Permission> findByCode(PermissionCode code) {
        return springDataIamPermissionRepository.findByCode(code.value())
                .map(iamPermissionMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByCode(PermissionCode code) {
        return springDataIamPermissionRepository.existsByCode(code.value());
    }
}
