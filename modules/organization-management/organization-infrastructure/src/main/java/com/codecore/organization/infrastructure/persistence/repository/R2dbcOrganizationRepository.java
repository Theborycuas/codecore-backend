package com.codecore.organization.infrastructure.persistence.repository;

import com.codecore.organization.application.port.out.OrganizationQueryPort;
import com.codecore.organization.application.port.out.OrganizationRepository;
import com.codecore.organization.domain.model.organization.Organization;
import com.codecore.organization.domain.valueobject.OrganizationCode;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.TenantId;
import com.codecore.organization.infrastructure.persistence.mapper.OrganizationMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Hexagonal adapter: implements outbound persistence ports using R2DBC.
 */
@Repository
public class R2dbcOrganizationRepository implements OrganizationRepository, OrganizationQueryPort {

    private final SpringDataOrganizationRepository springDataOrganizationRepository;
    private final OrganizationMapper organizationMapper;

    public R2dbcOrganizationRepository(
            SpringDataOrganizationRepository springDataOrganizationRepository,
            OrganizationMapper organizationMapper
    ) {
        this.springDataOrganizationRepository = springDataOrganizationRepository;
        this.organizationMapper = organizationMapper;
    }

    @Override
    public Mono<Organization> save(Organization organization) {
        return springDataOrganizationRepository
                .existsById(organization.id().value())
                .flatMap(exists -> springDataOrganizationRepository.save(
                        organizationMapper.toEntity(organization, !exists)))
                .map(organizationMapper::toDomain);
    }

    @Override
    public Mono<Organization> findById(OrganizationId id) {
        return springDataOrganizationRepository.findById(id.value())
                .map(organizationMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByTenantIdAndCode(TenantId tenantId, OrganizationCode code) {
        return springDataOrganizationRepository.existsByTenantIdAndCode(
                tenantId.value(),
                code.value()
        );
    }

    @Override
    public Flux<Organization> findAllByTenantId(TenantId tenantId) {
        return springDataOrganizationRepository.findAllByTenantId(tenantId.value())
                .map(organizationMapper::toDomain);
    }

    @Override
    public Mono<Organization> findByIdAndTenantId(OrganizationId id, TenantId tenantId) {
        return springDataOrganizationRepository.findByOrganizationIdAndTenantId(
                        id.value(),
                        tenantId.value()
                )
                .map(organizationMapper::toDomain);
    }

    @Override
    public Mono<Long> countByTenantId(TenantId tenantId) {
        return springDataOrganizationRepository.countByTenantId(tenantId.value());
    }
}
