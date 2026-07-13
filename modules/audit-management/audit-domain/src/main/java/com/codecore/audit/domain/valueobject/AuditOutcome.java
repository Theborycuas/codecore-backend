package com.codecore.audit.domain.valueobject;

/**
 * Outcome of the audited action (ADR-020). Defaults to {@link #SUCCESS} when omitted at append.
 */
public enum AuditOutcome {
    SUCCESS,
    FAILURE
}
