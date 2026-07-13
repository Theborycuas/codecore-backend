package com.codecore.access.domain.model.invitation;

import com.codecore.access.domain.exception.InvalidInvitationStateException;
import com.codecore.access.domain.valueobject.EmailAddress;
import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.domain.valueobject.InvitationRoleCode;
import com.codecore.access.domain.valueobject.InvitationStatus;
import com.codecore.access.domain.valueobject.InvitationTokenHash;
import com.codecore.access.domain.valueobject.MembershipId;
import com.codecore.access.domain.valueobject.TenantId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Invitation aggregate root — the intention to grant Membership in a Tenant to a recipient
 * identified by email (ADR-019).
 * <p>
 * <strong>One sentence:</strong> the pending (and resolved) intent that an identity obtain
 * Membership in a Tenant under conditions set by the inviter.
 * <p>
 * Intentionally small: tenant + email + system role (≠ OWNER) + inviter membership + token hash
 * + expiration + soft lifecycle PENDING/ACCEPTED/REVOKED/EXPIRED + optional resulting membership.
 * Never embeds StaffAssignment, Subscription, PasswordReset, Organization/Office scope, or email
 * transport — it never owns Membership.
 */
public final class Invitation {

    private final InvitationId id;
    private final TenantId tenantId;
    private final EmailAddress invitedEmail;
    private final InvitationRoleCode invitedRoleCode;
    private final MembershipId invitedByMembershipId;
    private final InvitationTokenHash tokenHash;
    private final Instant expiresAt;
    private InvitationStatus status;
    private MembershipId resultingMembershipId;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant acceptedAt;
    private Instant revokedAt;

    private Invitation(
            InvitationId id,
            TenantId tenantId,
            EmailAddress invitedEmail,
            InvitationRoleCode invitedRoleCode,
            MembershipId invitedByMembershipId,
            InvitationTokenHash tokenHash,
            Instant expiresAt,
            InvitationStatus status,
            MembershipId resultingMembershipId,
            Instant createdAt,
            Instant updatedAt,
            Instant acceptedAt,
            Instant revokedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.invitedEmail = Objects.requireNonNull(invitedEmail, "invitedEmail");
        this.invitedRoleCode = Objects.requireNonNull(invitedRoleCode, "invitedRoleCode");
        this.invitedByMembershipId = Objects.requireNonNull(invitedByMembershipId, "invitedByMembershipId");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.status = Objects.requireNonNull(status, "status");
        this.resultingMembershipId = resultingMembershipId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.acceptedAt = acceptedAt;
        this.revokedAt = revokedAt;
    }

    public static Invitation create(
            InvitationId id,
            TenantId tenantId,
            EmailAddress invitedEmail,
            InvitationRoleCode invitedRoleCode,
            MembershipId invitedByMembershipId,
            InvitationTokenHash tokenHash,
            Instant expiresAt,
            Instant now
    ) {
        Objects.requireNonNull(now, "now");
        return new Invitation(
                id,
                tenantId,
                invitedEmail,
                invitedRoleCode,
                invitedByMembershipId,
                tokenHash,
                expiresAt,
                InvitationStatus.PENDING,
                null,
                now,
                now,
                null,
                null
        );
    }

    public static Invitation reconstitute(
            InvitationId id,
            TenantId tenantId,
            EmailAddress invitedEmail,
            InvitationRoleCode invitedRoleCode,
            MembershipId invitedByMembershipId,
            InvitationTokenHash tokenHash,
            Instant expiresAt,
            InvitationStatus status,
            MembershipId resultingMembershipId,
            Instant createdAt,
            Instant updatedAt,
            Instant acceptedAt,
            Instant revokedAt
    ) {
        return new Invitation(
                id,
                tenantId,
                invitedEmail,
                invitedRoleCode,
                invitedByMembershipId,
                tokenHash,
                expiresAt,
                status,
                resultingMembershipId,
                createdAt,
                updatedAt,
                acceptedAt,
                revokedAt
        );
    }

    public InvitationId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public EmailAddress invitedEmail() {
        return invitedEmail;
    }

    public InvitationRoleCode invitedRoleCode() {
        return invitedRoleCode;
    }

    public MembershipId invitedByMembershipId() {
        return invitedByMembershipId;
    }

    public InvitationTokenHash tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public InvitationStatus status() {
        return status;
    }

    public Optional<MembershipId> resultingMembershipId() {
        return Optional.ofNullable(resultingMembershipId);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Optional<Instant> acceptedAt() {
        return Optional.ofNullable(acceptedAt);
    }

    public Optional<Instant> revokedAt() {
        return Optional.ofNullable(revokedAt);
    }

    /**
     * {@code PENDING -> ACCEPTED}. Requires {@code now < expiresAt}; sets resulting membership
     * set-once. Application provisions Membership via IAM before calling this (ADR-019).
     */
    public void accept(Instant now, MembershipId resultingMembershipId) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(resultingMembershipId, "resultingMembershipId");
        requirePending("accept");
        if (!now.isBefore(expiresAt)) {
            throw new InvalidInvitationStateException("Cannot accept an expired invitation");
        }
        this.status = InvitationStatus.ACCEPTED;
        this.resultingMembershipId = resultingMembershipId;
        this.acceptedAt = now;
        touch(now);
    }

    /** {@code PENDING -> REVOKED}. No un-revoke; no ReferencePort re-validation. */
    public void revoke(Instant now) {
        Objects.requireNonNull(now, "now");
        requirePending("revoke");
        this.status = InvitationStatus.REVOKED;
        this.revokedAt = now;
        touch(now);
    }

    /** {@code PENDING -> EXPIRED}. Typically when {@code now >= expiresAt} (job or accept attempt). */
    public void expire(Instant now) {
        Objects.requireNonNull(now, "now");
        requirePending("expire");
        this.status = InvitationStatus.EXPIRED;
        touch(now);
    }

    private void requirePending(String action) {
        if (status != InvitationStatus.PENDING) {
            throw new InvalidInvitationStateException(
                    "Cannot " + action + " when invitation is " + status
            );
        }
    }

    private void touch(Instant now) {
        this.updatedAt = now;
    }
}
