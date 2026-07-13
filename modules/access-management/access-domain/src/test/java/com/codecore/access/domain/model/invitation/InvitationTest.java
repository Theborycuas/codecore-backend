package com.codecore.access.domain.model.invitation;

import com.codecore.access.domain.exception.InvalidInvitationStateException;
import com.codecore.access.domain.valueobject.EmailAddress;
import com.codecore.access.domain.valueobject.InvitationId;
import com.codecore.access.domain.valueobject.InvitationRoleCode;
import com.codecore.access.domain.valueobject.InvitationStatus;
import com.codecore.access.domain.valueobject.InvitationTokenHash;
import com.codecore.access.domain.valueobject.MembershipId;
import com.codecore.access.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvitationTest {

    private static final Instant NOW = Instant.parse("2026-07-12T18:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-07-19T18:00:00Z");

    @Test
    void shouldCreatePendingInvitation() {
        InvitationId id = InvitationId.generate();
        TenantId tenantId = TenantId.generate();
        EmailAddress email = EmailAddress.of("invitee@example.com");
        InvitationRoleCode role = InvitationRoleCode.of("ADMIN");
        MembershipId invitedBy = MembershipId.of(UUID.randomUUID());
        InvitationTokenHash tokenHash = InvitationTokenHash.ofHashedValue("hashed-token");

        Invitation invitation = Invitation.create(
                id, tenantId, email, role, invitedBy, tokenHash, EXPIRES_AT, NOW
        );

        assertThat(invitation.id()).isEqualTo(id);
        assertThat(invitation.tenantId()).isEqualTo(tenantId);
        assertThat(invitation.invitedEmail()).isEqualTo(email);
        assertThat(invitation.invitedRoleCode()).isEqualTo(role);
        assertThat(invitation.invitedByMembershipId()).isEqualTo(invitedBy);
        assertThat(invitation.tokenHash()).isEqualTo(tokenHash);
        assertThat(invitation.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(invitation.status()).isEqualTo(InvitationStatus.PENDING);
        assertThat(invitation.resultingMembershipId()).isEmpty();
        assertThat(invitation.createdAt()).isEqualTo(NOW);
        assertThat(invitation.updatedAt()).isEqualTo(NOW);
        assertThat(invitation.acceptedAt()).isEmpty();
        assertThat(invitation.revokedAt()).isEmpty();
    }

    @Test
    void shouldAcceptFromPending() {
        Invitation invitation = pendingInvitation();
        MembershipId resulting = MembershipId.of(UUID.randomUUID());
        Instant acceptedAt = NOW.plusSeconds(60);

        invitation.accept(acceptedAt, resulting);

        assertThat(invitation.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.resultingMembershipId()).contains(resulting);
        assertThat(invitation.acceptedAt()).contains(acceptedAt);
        assertThat(invitation.updatedAt()).isEqualTo(acceptedAt);
    }

    @Test
    void shouldRevokeFromPending() {
        Invitation invitation = pendingInvitation();
        Instant revokedAt = NOW.plusSeconds(30);

        invitation.revoke(revokedAt);

        assertThat(invitation.status()).isEqualTo(InvitationStatus.REVOKED);
        assertThat(invitation.revokedAt()).contains(revokedAt);
        assertThat(invitation.updatedAt()).isEqualTo(revokedAt);
        assertThat(invitation.resultingMembershipId()).isEmpty();
    }

    @Test
    void shouldExpireFromPending() {
        Invitation invitation = pendingInvitation();
        Instant expiredAt = EXPIRES_AT.plusSeconds(1);

        invitation.expire(expiredAt);

        assertThat(invitation.status()).isEqualTo(InvitationStatus.EXPIRED);
        assertThat(invitation.updatedAt()).isEqualTo(expiredAt);
        assertThat(invitation.acceptedAt()).isEmpty();
        assertThat(invitation.revokedAt()).isEmpty();
    }

    @Test
    void shouldRejectAcceptWhenRevoked() {
        Invitation invitation = pendingInvitation();
        invitation.revoke(NOW.plusSeconds(10));

        assertThatThrownBy(() -> invitation.accept(NOW.plusSeconds(20), MembershipId.generate()))
                .isInstanceOf(InvalidInvitationStateException.class)
                .hasMessageContaining("REVOKED");
    }

    @Test
    void shouldRejectAcceptWhenAlreadyAccepted() {
        Invitation invitation = pendingInvitation();
        invitation.accept(NOW.plusSeconds(10), MembershipId.generate());

        assertThatThrownBy(() -> invitation.accept(NOW.plusSeconds(20), MembershipId.generate()))
                .isInstanceOf(InvalidInvitationStateException.class)
                .hasMessageContaining("ACCEPTED");
    }

    @Test
    void shouldRejectAcceptWhenExpiredByClock() {
        Invitation invitation = pendingInvitation();

        assertThatThrownBy(() -> invitation.accept(EXPIRES_AT, MembershipId.generate()))
                .isInstanceOf(InvalidInvitationStateException.class)
                .hasMessageContaining("expired");
        assertThat(invitation.status()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void shouldRejectAcceptWhenStatusExpired() {
        Invitation invitation = pendingInvitation();
        invitation.expire(EXPIRES_AT);

        assertThatThrownBy(() -> invitation.accept(EXPIRES_AT.plusSeconds(1), MembershipId.generate()))
                .isInstanceOf(InvalidInvitationStateException.class)
                .hasMessageContaining("EXPIRED");
    }

    @Test
    void shouldRejectRevokeWhenAccepted() {
        Invitation invitation = pendingInvitation();
        invitation.accept(NOW.plusSeconds(10), MembershipId.generate());

        assertThatThrownBy(() -> invitation.revoke(NOW.plusSeconds(20)))
                .isInstanceOf(InvalidInvitationStateException.class)
                .hasMessageContaining("ACCEPTED");
    }

    @Test
    void shouldSetResultingMembershipIdOnceOnAccept() {
        Invitation invitation = pendingInvitation();
        MembershipId resulting = MembershipId.of(UUID.randomUUID());

        invitation.accept(NOW.plusSeconds(5), resulting);

        assertThat(invitation.resultingMembershipId()).contains(resulting);
        assertThatThrownBy(() -> invitation.accept(NOW.plusSeconds(10), MembershipId.generate()))
                .isInstanceOf(InvalidInvitationStateException.class);
        assertThat(invitation.resultingMembershipId()).contains(resulting);
    }

    @Test
    void shouldKeepTenantIdAndEmailImmutableAfterAccept() {
        Invitation invitation = pendingInvitation();
        TenantId originalTenant = invitation.tenantId();
        EmailAddress originalEmail = invitation.invitedEmail();

        invitation.accept(NOW.plusSeconds(5), MembershipId.generate());

        assertThat(invitation.tenantId()).isEqualTo(originalTenant);
        assertThat(invitation.invitedEmail()).isEqualTo(originalEmail);
    }

    @Test
    void shouldReconstituteInvitation() {
        InvitationId id = InvitationId.generate();
        TenantId tenantId = TenantId.generate();
        EmailAddress email = EmailAddress.of("restored@example.com");
        InvitationRoleCode role = InvitationRoleCode.of("USER");
        MembershipId invitedBy = MembershipId.of(UUID.randomUUID());
        MembershipId resulting = MembershipId.of(UUID.randomUUID());
        Instant createdAt = NOW.minusSeconds(3600);
        Instant updatedAt = NOW.minusSeconds(60);
        Instant acceptedAt = NOW.minusSeconds(60);

        Invitation invitation = Invitation.reconstitute(
                id,
                tenantId,
                email,
                role,
                invitedBy,
                InvitationTokenHash.ofHashedValue("stored-hash"),
                EXPIRES_AT,
                InvitationStatus.ACCEPTED,
                resulting,
                createdAt,
                updatedAt,
                acceptedAt,
                null
        );

        assertThat(invitation.id()).isEqualTo(id);
        assertThat(invitation.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.resultingMembershipId()).contains(resulting);
        assertThat(invitation.acceptedAt()).contains(acceptedAt);
        assertThat(invitation.revokedAt()).isEmpty();
        assertThat(invitation.createdAt()).isEqualTo(createdAt);
        assertThat(invitation.updatedAt()).isEqualTo(updatedAt);
    }

    private static Invitation pendingInvitation() {
        return Invitation.create(
                InvitationId.generate(),
                TenantId.generate(),
                EmailAddress.of("invitee@example.com"),
                InvitationRoleCode.of("MANAGER"),
                MembershipId.of(UUID.randomUUID()),
                InvitationTokenHash.ofHashedValue("hashed-token"),
                EXPIRES_AT,
                NOW
        );
    }
}
