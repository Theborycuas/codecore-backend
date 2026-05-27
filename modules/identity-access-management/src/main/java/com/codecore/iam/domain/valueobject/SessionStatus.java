package com.codecore.iam.domain.valueobject;

/**
 * Session lifecycle state for {@link com.codecore.iam.domain.model.session.Session}.
 */
public enum SessionStatus {
    ACTIVE,
    REVOKED,
    EXPIRED,
    INVALIDATED;

    public boolean mayRefresh() {
        return this == ACTIVE;
    }
}
