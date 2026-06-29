package com.codecore.organization.infrastructure.persistence.mapper;

import com.codecore.organization.domain.model.staffassignment.StaffAssignment;
import com.codecore.organization.domain.valueobject.MembershipId;
import com.codecore.organization.domain.valueobject.OfficeId;
import com.codecore.organization.domain.valueobject.OrganizationId;
import com.codecore.organization.domain.valueobject.StaffAssignmentId;
import com.codecore.organization.domain.valueobject.TenantId;
import com.codecore.organization.infrastructure.persistence.entity.StaffAssignmentEntity;

public final class StaffAssignmentMapper {

    public StaffAssignment toDomain(StaffAssignmentEntity entity) {
        return StaffAssignment.reconstitute(
                new StaffAssignmentId(entity.getAssignmentId()),
                new TenantId(entity.getTenantId()),
                new MembershipId(entity.getMembershipId()),
                new OrganizationId(entity.getOrganizationId()),
                entity.getOfficeId() != null ? new OfficeId(entity.getOfficeId()) : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public StaffAssignmentEntity toEntity(StaffAssignment assignment, boolean isNew) {
        StaffAssignmentEntity entity = new StaffAssignmentEntity();
        entity.setNewEntity(isNew);
        entity.setAssignmentId(assignment.id().value());
        entity.setTenantId(assignment.tenantId().value());
        entity.setMembershipId(assignment.membershipId().value());
        entity.setOrganizationId(assignment.organizationId().value());
        entity.setOfficeId(assignment.officeId() != null ? assignment.officeId().value() : null);
        entity.setCreatedAt(assignment.createdAt());
        entity.setUpdatedAt(assignment.updatedAt());
        return entity;
    }
}
