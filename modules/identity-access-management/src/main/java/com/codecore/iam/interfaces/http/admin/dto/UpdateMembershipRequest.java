package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.domain.valueobject.MembershipStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateMembershipRequest(
        @NotNull MembershipStatus status
) {
}
