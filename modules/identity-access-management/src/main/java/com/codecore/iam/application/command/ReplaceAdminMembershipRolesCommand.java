package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.MembershipId;

import java.util.List;
import java.util.UUID;

public record ReplaceAdminMembershipRolesCommand(
        MembershipId membershipId,
        List<UUID> roleIds
) {
}
