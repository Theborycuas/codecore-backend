package com.codecore.iam.domain.model.session;

/**
 * Non-invariant client metadata captured at session creation (audit / risk analysis).
 * Validated at the application or infrastructure boundary when required.
 */
public record ClientContext(String deviceId, String clientIp, String userAgent) {

    public static ClientContext unknown() {
        return new ClientContext("unknown", "unknown", "unknown");
    }
}
