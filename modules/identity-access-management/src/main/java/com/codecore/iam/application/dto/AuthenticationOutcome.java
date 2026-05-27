package com.codecore.iam.application.dto;

public enum AuthenticationOutcome {
    SUCCESS,
    INVALID_CREDENTIALS,
    LOCKED_ACCOUNT,
    DISABLED_ACCOUNT,
    PASSWORD_RESET_REQUIRED,
    PENDING_VERIFICATION,
    TOKEN_EXPIRED,
    UNAUTHORIZED
}
