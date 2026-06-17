package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.domain.valueobject.MembershipStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateMembershipRequest(
        UUID identityId,
        @Email String email,
        @Size(min = 8, max = 128) String password
) {
}
