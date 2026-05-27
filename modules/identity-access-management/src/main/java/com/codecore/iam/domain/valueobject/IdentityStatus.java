package com.codecore.iam.domain.valueobject;

/**
 * Authentication lifecycle state for {@link com.codecore.iam.domain.model.identity.Identity}.
 */
public enum IdentityStatus {
    ACTIVE,
    LOCKED,
    DISABLED,
    PENDING_VERIFICATION,
    PASSWORD_RESET_REQUIRED;

    public boolean mayAuthenticate() {
        return this == ACTIVE;
    }

    public boolean isLocked() {
        return this == LOCKED;
    }
}
