package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.application.dto.AdminMembershipView;

import java.time.Instant;
import java.util.UUID;

public record MembershipResponse(
        UUID id,
        UUID identityId,
        UUID tenantId,
        String status,
        String identityEmail,
        Instant createdAt,
        Instant updatedAt
) {

    public static MembershipResponse from(AdminMembershipView view) {
        return new MembershipResponse(
                view.id().value(),
                view.identityId().value(),
                view.tenantId().value(),
                view.status().name(),
                view.identityEmail(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
