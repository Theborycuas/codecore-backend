package com.codecore.access.contract.authorization;

import java.util.Set;

/**
 * Access (Invitation) permission contract (ADR-019 / FASE 23.5).
 * <p>
 * String codes are the canonical identifiers seeded in {@code iam.permission}.
 * IAM maps them to {@code PermissionCode} via {@code IamPermissionCatalog}.
 * <p>
 * Intentionally limited to the invitation lifecycle — no {@code invitation:resend},
 * {@code invitation:update}, or seat/subscription grants.
 */
public final class InvitationPermissionCatalog {

    public static final String INVITATION_CREATE = "invitation:create";
    public static final String INVITATION_READ = "invitation:read";
    public static final String INVITATION_REVOKE = "invitation:revoke";

    /** Full Invitation lifecycle contract (FASE 23 — create / read / revoke). */
    public static final Set<String> ALL = Set.of(
            INVITATION_CREATE,
            INVITATION_READ,
            INVITATION_REVOKE
    );

    /** Read-only consultation of invitations. */
    public static final Set<String> INVITATION_READ_ONLY = Set.of(INVITATION_READ);

    private InvitationPermissionCatalog() {
    }
}
