package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.MembershipRepository;
import com.codecore.iam.domain.model.membership.IdentityTenantMembership;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.persistence.mapper.IamIdentityTenantMembershipMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Hexagonal adapter: implements the application outbound port using R2DBC.
 */
@Repository
public class R2dbcMembershipRepository implements MembershipRepository {

    private final SpringDataIamIdentityTenantMembershipRepository springDataRepository;
    private final IamIdentityTenantMembershipMapper mapper;

    public R2dbcMembershipRepository(
            SpringDataIamIdentityTenantMembershipRepository springDataRepository,
            IamIdentityTenantMembershipMapper membershipMapper
    ) {
        this.springDataRepository = springDataRepository;
        this.mapper = membershipMapper;
    }

    @Override
    public Mono<IdentityTenantMembership> save(IdentityTenantMembership membership) {
        return springDataRepository
                .existsById(membership.id().value())
                .flatMap(exists -> springDataRepository.save(
                        mapper.toEntity(membership, !exists)))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> exists(IdentityId identityId, TenantId tenantId) {
        return springDataRepository.existsByIdentityIdAndTenantId(
                identityId.value(),
                tenantId.value()
        );
    }

    @Override
    public Flux<IdentityTenantMembership> findByIdentityId(IdentityId identityId) {
        return springDataRepository.findByIdentityId(identityId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Flux<IdentityTenantMembership> findByTenantId(TenantId tenantId) {
        return springDataRepository.findByTenantId(tenantId.value())
                .map(mapper::toDomain);
    }
}
