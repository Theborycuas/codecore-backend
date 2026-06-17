package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.domain.valueobject.IdentityStatus;

public record UpdateUserRequest(
        IdentityStatus status,
        String email
) {
}
