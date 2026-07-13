package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.RawPassword;
import com.codecore.iam.domain.valueobject.ResetTokenHash;
import com.codecore.iam.domain.valueobject.TenantId;

/**
 * @param tenantId optional; when present must match the stored reset request tenant
 */
public record CompletePasswordResetCommand(
        TenantId tenantId,
        ResetTokenHash resetTokenHash,
        RawPassword newPassword
) {
}
