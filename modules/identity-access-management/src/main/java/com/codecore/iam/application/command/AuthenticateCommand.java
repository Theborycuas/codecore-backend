package com.codecore.iam.application.command;

import com.codecore.iam.domain.valueobject.EmailAddress;
import com.codecore.iam.domain.valueobject.RawPassword;
import com.codecore.iam.domain.valueobject.TenantId;

public record AuthenticateCommand(
        TenantId tenantId,
        EmailAddress email,
        RawPassword password
) {
}
