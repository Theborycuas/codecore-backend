package com.codecore.iam.interfaces.http.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReplaceMembershipRolesRequest(
        @NotNull List<UUID> roleIds
) {
}
