package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.TenantRepository;
import com.codecore.iam.domain.model.tenant.Tenant;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.domain.valueobject.TenantName;
import com.codecore.iam.infrastructure.persistence.mapper.IamTenantMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Hexagonal adapter: implements the application outbound port using R2DBC.
 */
@Repository
public class R2dbcTenantRepository implements TenantRepository {

    private final SpringDataIamTenantRepository springDataIamTenantRepository;
    private final IamTenantMapper iamTenantMapper;

    public R2dbcTenantRepository(
            SpringDataIamTenantRepository springDataIamTenantRepository,
            IamTenantMapper iamTenantMapper
    ) {
        this.springDataIamTenantRepository = springDataIamTenantRepository;
        this.iamTenantMapper = iamTenantMapper;
    }

    @Override
    public Mono<Tenant> save(Tenant tenant) {
        return springDataIamTenantRepository
                .existsById(tenant.id().value())
                .flatMap(exists -> springDataIamTenantRepository.save(
                        iamTenantMapper.toEntity(tenant, !exists)))
                .map(iamTenantMapper::toDomain);
    }

    @Override
    public Mono<Tenant> findById(TenantId tenantId) {
        return springDataIamTenantRepository.findById(tenantId.value())
                .map(iamTenantMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsById(TenantId tenantId) {
        return springDataIamTenantRepository.existsById(tenantId.value());
    }

    @Override
    public Mono<Boolean> existsByName(TenantName name) {
        return springDataIamTenantRepository.existsByName(name.value());
    }

    @Override
    public Mono<Long> count() {
        return springDataIamTenantRepository.count();
    }
}
