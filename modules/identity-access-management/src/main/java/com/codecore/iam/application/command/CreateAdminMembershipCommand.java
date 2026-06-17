package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.IdentityId;
import com.codecore.iam.domain.valueobject.MembershipStatus;

import java.util.UUID;

public record CreateAdminMembershipCommand(
        UUID identityId,
        String email,
        String password
) {
    public IdentityId identityIdValue() {
        return identityId != null ? new IdentityId(identityId) : null;
    }
}
