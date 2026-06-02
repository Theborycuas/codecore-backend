package com.codecore.iam.infrastructure.persistence;

import com.codecore.iam.domain.valueobject.IdentityStatus;

/**
 * Formula for the {@code iam.iam_user.email_verified} column.
 * <p>
 * <strong>Not a source of truth.</strong> Master data is {@link IdentityStatus} on the
 * {@link com.codecore.iam.domain.model.identity.Identity} aggregate ({@code status} column).
 * This value is a <em>persisted projection</em> for SQL filters, indexes, and reporting only.
 * <p>
 * Inbound (entity → domain): never use to reconstruct lifecycle — read {@code status} only.
 * Outbound (domain → entity): use {@link #fromStatus(IdentityStatus)} when writing the row.
 */
public final class EmailVerifiedProjection {

    private EmailVerifiedProjection() {
    }

    public static boolean fromStatus(IdentityStatus status) {
        return status != IdentityStatus.PENDING_VERIFICATION;
    }
}
