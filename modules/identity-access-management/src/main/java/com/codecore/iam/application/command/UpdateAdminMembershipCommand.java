package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.MembershipId;
import com.codecore.iam.domain.valueobject.MembershipStatus;

public record UpdateAdminMembershipCommand(
        MembershipId membershipId,
        MembershipStatus status
) {
}
