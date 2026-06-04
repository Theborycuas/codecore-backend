package com.codecore.iam.domain.exception;

import java.util.Objects;

/**
 * Raised when {@link com.codecore.iam.application.port.out.TenantContext} cannot resolve a tenant
 * for the current request.
 */
public class TenantContextUnavailableException extends IamDomainException {

    public enum Reason {
        NOT_AUTHENTICATED,
        TENANT_CLAIM_ABSENT
    }

    private final Reason reason;

    public TenantContextUnavailableException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public Reason reason() {
        return reason;
    }
}
