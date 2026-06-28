package com.codecore.organization.infrastructure.persistence.repository;

import com.codecore.organization.application.port.out.OfficeQueryPort;
import com.codecore.organization.application.port.out.OfficeRepository;
import com.codecore.organization.domain.model.office.Office;
import com.codecore.organization.domain.valueobject.OfficeCode;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OfficeStatus;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.infrastructure.persistence.mapper.OfficeMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcOfficeRepository implements OfficeRepository, OfficeQueryPort {

    private final SpringDataOfficeRepository springDataOfficeRepository;
    private final OfficeMapper officeMapper;

    public R2dbcOfficeRepository(
            SpringDataOfficeRepository springDataOfficeRepository,
            OfficeMapper officeMapper
    ) {
        this.springDataOfficeRepository = springDataOfficeRepository;
        this.officeMapper = officeMapper;
    }

    @Override
    public Mono<Office> save(Office office) {
        return springDataOfficeRepository
                .existsById(office.id().value())
                .flatMap(exists -> springDataOfficeRepository.save(
                        officeMapper.toEntity(office, !exists)))
                .map(officeMapper::toDomain);
    }

    @Override
    public Mono<Office> findById(OfficeId id) {
        return springDataOfficeRepository.findById(id.value())
                .map(officeMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByOrganizationIdAndCode(OrganizationId organizationId, OfficeCode code) {
        return springDataOfficeRepository.existsByOrganizationIdAndCode(
                organizationId.value(),
                code.value()
        );
    }

    @Override
    public Mono<Long> countActiveByOrganizationId(OrganizationId organizationId) {
        return springDataOfficeRepository.countByOrganizationIdAndStatus(
                organizationId.value(),
                OfficeStatus.ACTIVE.name()
        );
    }

    @Override
    public Mono<Office> findByIdAndTenantId(OfficeId id, com.codecore.organization.domain.valueobject.TenantId tenantId) {
        return springDataOfficeRepository.findByOfficeIdAndTenantId(id.value(), tenantId.value())
                .map(officeMapper::toDomain);
    }
}
