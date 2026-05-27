package com.codecore.iam.domain.valueobject;

/**
 * Password reset request lifecycle (blueprint: entities.md §8.4).
 */
public enum PasswordResetStatus {
    PENDING,
    USED,
    EXPIRED,
    CANCELLED;

    public boolean mayCompleteReset() {
        return this == PENDING;
    }
}
