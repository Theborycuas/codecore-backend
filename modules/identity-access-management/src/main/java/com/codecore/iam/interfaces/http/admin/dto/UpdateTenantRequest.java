package com.codecore.iam.interfaces.http.admin.dto;

import com.codecore.iam.domain.valueobject.TenantStatus;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
        @Size(max = 200) String name,
        TenantStatus status
) {
}
