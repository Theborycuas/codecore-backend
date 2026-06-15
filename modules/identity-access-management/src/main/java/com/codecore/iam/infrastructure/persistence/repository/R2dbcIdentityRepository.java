package com.codecore.iam.infrastructure.persistence.repository;

import com.codecore.iam.application.port.out.IdentityRepository;
import com.codecore.iam.domain.model.identity.Identity;
import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.TenantId;
import com.codecore.iam.infrastructure.persistence.mapper.IamUserMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Hexagonal adapter: implements the application outbound port using R2DBC.
 */
@Repository
public class R2dbcIdentityRepository implements IdentityRepository {

    private final SpringDataIamUserRepository springDataIamUserRepository;
    private final IamUserMapper iamUserMapper;

    public R2dbcIdentityRepository(
            SpringDataIamUserRepository springDataIamUserRepository,
            IamUserMapper iamUserMapper
    ) {
        this.springDataIamUserRepository = springDataIamUserRepository;
        this.iamUserMapper = iamUserMapper;
    }

    @Override
    public Mono<Identity> save(Identity identity) {
        return springDataIamUserRepository
                .findByTenantIdAndId(identity.tenantId().value(), identity.id().value())
                .hasElement()
                .flatMap(exists -> springDataIamUserRepository.save(
                        iamUserMapper.toEntity(identity, !exists)))
                .map(iamUserMapper::toDomain);
    }

    @Override
    public Mono<Identity> findById(TenantId tenantId, IdentityId identityId) {
        return springDataIamUserRepository.findByTenantIdAndId(tenantId.value(), identityId.value())
                .map(iamUserMapper::toDomain);
    }

    @Override
    public Mono<Identity> findByEmail(EmailAddress email) {
        return springDataIamUserRepository.findFirstByNormalizedEmailOrderByCreatedAtAsc(email.value())
                .map(iamUserMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByEmail(EmailAddress email) {
        return springDataIamUserRepository.existsByNormalizedEmail(email.value());
    }

    @Override
    public Mono<Identity> findByTenantAndEmail(TenantId tenantId, EmailAddress email) {
        return springDataIamUserRepository.findByTenantIdAndNormalizedEmail(tenantId.value(), email.value())
                .map(iamUserMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByTenantAndEmail(TenantId tenantId, EmailAddress email) {
        return springDataIamUserRepository.existsByTenantIdAndNormalizedEmail(tenantId.value(), email.value());
    }

    @Override
    public Mono<Void> delete(TenantId tenantId, IdentityId identityId) {
        return springDataIamUserRepository.findByTenantIdAndId(tenantId.value(), identityId.value())
                .flatMap(springDataIamUserRepository::delete)
                .then();
    }
}
