package com.codecore.iam.domain.exception;

import com.codecore.iam.domain.valueobject.TenantStatus;

/**
 * Tenant exists but is not in an operational state (login / API access blocked).
 */
public final class TenantNotOperationalException extends RuntimeException {

    private final TenantStatus status;

    public TenantNotOperationalException(TenantStatus status) {
        super("Tenant is not operational: " + status);
        this.status = status;
    }

    public TenantStatus status() {
        return status;
    }
}
