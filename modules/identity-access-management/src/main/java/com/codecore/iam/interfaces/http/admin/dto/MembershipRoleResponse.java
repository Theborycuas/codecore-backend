package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.application.dto.AdminMembershipRoleView;

import java.time.Instant;
import java.util.UUID;

public record MembershipRoleResponse(
        UUID roleId,
        String code,
        String name,
        String status,
        boolean systemRole,
        Instant assignedAt
) {

    public static MembershipRoleResponse from(AdminMembershipRoleView view) {
        return new MembershipRoleResponse(
                view.roleId().value(),
                view.code(),
                view.name(),
                view.status(),
                view.systemRole(),
                view.assignedAt()
        );
    }
}
