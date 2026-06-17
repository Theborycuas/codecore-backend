package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.TenantStatus;

public record UpdateAdminTenantCommand(
        String name,
        TenantStatus status
) {
}
