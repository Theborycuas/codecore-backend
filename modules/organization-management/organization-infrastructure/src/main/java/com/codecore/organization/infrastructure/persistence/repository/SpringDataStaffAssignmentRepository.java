package com.codecore.organization.infrastructure.persistence.repository;

import com.codecore.organization.infrastructure.persistence.entity.StaffAssignmentEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SpringDataStaffAssignmentRepository extends ReactiveCrudRepository<StaffAssignmentEntity, UUID> {

    Mono<StaffAssignmentEntity> findByAssignmentIdAndTenantId(UUID assignmentId, UUID tenantId);

    Mono<Boolean> existsByTenantIdAndMembershipIdAndOrganizationIdAndOfficeIdIsNull(
            UUID tenantId,
            UUID membershipId,
            UUID organizationId
    );

    Mono<Boolean> existsByTenantIdAndMembershipIdAndOfficeId(
            UUID tenantId,
            UUID membershipId,
            UUID officeId
    );
}
