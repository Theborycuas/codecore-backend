package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.RoleRepository;
import com.codecore.iam.domain.model.role.Role;
import com.codecore.iam.domain.valueobject.RoleCode;
import com.codecore.iam.domain.valueobject.RoleId;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.persistence.mapper.IamRoleMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Hexagonal adapter: implements the application outbound port using R2DBC.
 */
@Repository
public class R2dbcRoleRepository implements RoleRepository {

    private final SpringDataIamRoleRepository springDataIamRoleRepository;
    private final IamRoleMapper iamRoleMapper;

    public R2dbcRoleRepository(
            SpringDataIamRoleRepository springDataIamRoleRepository,
            IamRoleMapper iamRoleMapper
    ) {
        this.springDataIamRoleRepository = springDataIamRoleRepository;
        this.iamRoleMapper = iamRoleMapper;
    }

    @Override
    public Mono<Role> save(Role role) {
        return springDataIamRoleRepository
                .existsById(role.id().value())
                .flatMap(exists -> springDataIamRoleRepository.save(
                        iamRoleMapper.toEntity(role, !exists)))
                .map(iamRoleMapper::toDomain);
    }

    @Override
    public Mono<Role> findById(RoleId roleId) {
        return springDataIamRoleRepository.findById(roleId.value())
                .map(iamRoleMapper::toDomain);
    }

    @Override
    public Mono<Role> findByTenantIdAndCode(TenantId tenantId, RoleCode code) {
        return springDataIamRoleRepository.findByTenantIdAndCode(tenantId.value(), code.value())
                .map(iamRoleMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByTenantIdAndCode(TenantId tenantId, RoleCode code) {
        return springDataIamRoleRepository.existsByTenantIdAndCode(tenantId.value(), code.value());
    }

    @Override
    public Mono<Void> delete(RoleId roleId) {
        return springDataIamRoleRepository.deleteById(roleId.value()).then();
    }
}
