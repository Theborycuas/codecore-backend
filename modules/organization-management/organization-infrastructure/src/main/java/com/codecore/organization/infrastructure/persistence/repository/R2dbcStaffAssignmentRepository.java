package com.codecore.organization.infrastructure.persistence.repository;

import com.codecore.organization.application.port.out.StaffAssignmentQueryPort;
import com.codecore.organization.application.port.out.StaffAssignmentRepository;
import com.codecore.organization.domain.model.staffassignment.StaffAssignment;
import com.codecore.organization.domain.valueobject.MembershipId;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import com.codecore.organization.domain.valueobject.TenantId;
import com.codecore.organization.infrastructure.persistence.mapper.StaffAssignmentMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcStaffAssignmentRepository implements StaffAssignmentRepository, StaffAssignmentQueryPort {

    private final SpringDataStaffAssignmentRepository springDataStaffAssignmentRepository;
    private final StaffAssignmentMapper staffAssignmentMapper;

    public R2dbcStaffAssignmentRepository(
            SpringDataStaffAssignmentRepository springDataStaffAssignmentRepository,
            StaffAssignmentMapper staffAssignmentMapper
    ) {
        this.springDataStaffAssignmentRepository = springDataStaffAssignmentRepository;
        this.staffAssignmentMapper = staffAssignmentMapper;
    }

    @Override
    public Mono<StaffAssignment> save(StaffAssignment assignment) {
        return springDataStaffAssignmentRepository
                .existsById(assignment.id().value())
                .flatMap(exists -> springDataStaffAssignmentRepository.save(
                        staffAssignmentMapper.toEntity(assignment, !exists)))
                .map(staffAssignmentMapper::toDomain);
    }

    @Override
    public Mono<Boolean> existsByScope(
            TenantId tenantId,
            MembershipId membershipId,
            OrganizationId organizationId,
            OfficeId officeId
    ) {
        if (officeId == null) {
            return springDataStaffAssignmentRepository.existsByTenantIdAndMembershipIdAndOrganizationIdAndOfficeIdIsNull(
                    tenantId.value(),
                    membershipId.value(),
                    organizationId.value()
            );
        }
        return springDataStaffAssignmentRepository.existsByTenantIdAndMembershipIdAndOfficeId(
                tenantId.value(),
                membershipId.value(),
                officeId.value()
        );
    }

    @Override
    public Mono<Void> delete(StaffAssignmentId id) {
        return springDataStaffAssignmentRepository.deleteById(id.value());
    }

    @Override
    public Mono<StaffAssignment> findByIdAndTenantId(StaffAssignmentId id, TenantId tenantId) {
        return springDataStaffAssignmentRepository
                .findByAssignmentIdAndTenantId(id.value(), tenantId.value())
                .map(staffAssignmentMapper::toDomain);
    }
}
