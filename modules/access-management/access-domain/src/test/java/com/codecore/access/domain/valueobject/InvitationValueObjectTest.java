package com.codecore.access.domain.valueobject;

import com.codecore.access.domain.exception.InvalidDomainValueException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvitationValueObjectTest {

    @Test
    void shouldGenerateDistinctInvitationIds() {
        assertThat(InvitationId.generate()).isNotEqualTo(InvitationId.generate());
    }

    @Test
    void shouldParseInvitationIdFromString() {
        UUID uuid = UUID.randomUUID();
        assertThat(new InvitationId(uuid.toString()).value()).isEqualTo(uuid);
    }

    @Test
    void shouldEqualTenantIdsByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(new TenantId(uuid)).isEqualTo(new TenantId(uuid.toString()));
    }

    @Test
    void shouldEqualMembershipIdsByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(MembershipId.of(uuid)).isEqualTo(new MembershipId(uuid.toString()));
    }

    @Test
    void shouldNormalizeEmailAddress() {
        EmailAddress email = EmailAddress.of("  Invitee@Example.COM  ");
        assertThat(email.value()).isEqualTo("invitee@example.com");
    }

    @Test
    void shouldRejectEmailWithoutAtSign() {
        assertThatThrownBy(() -> EmailAddress.of("not-an-email"))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("email");
    }

    @Test
    void shouldRejectBlankEmail() {
        assertThatThrownBy(() -> EmailAddress.of("   "))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("email");
    }

    @Test
    void shouldAcceptAllowedInvitationRoleCodes() {
        assertThat(InvitationRoleCode.of("admin").value()).isEqualTo("ADMIN");
        assertThat(InvitationRoleCode.of("MANAGER").value()).isEqualTo("MANAGER");
        assertThat(InvitationRoleCode.of("user").value()).isEqualTo("USER");
        assertThat(InvitationRoleCode.of("READ_ONLY").value()).isEqualTo("READ_ONLY");
    }

    @Test
    void shouldRejectOwnerInvitationRoleCode() {
        assertThatThrownBy(() -> InvitationRoleCode.of("OWNER"))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("OWNER");
    }

    @Test
    void shouldRejectUnknownInvitationRoleCode() {
        assertThatThrownBy(() -> InvitationRoleCode.of("SUPERUSER"))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("SUPERUSER");
    }

    @Test
    void shouldCreateTokenHashFromHashedValue() {
        assertThat(InvitationTokenHash.ofHashedValue("  abc123  ").value()).isEqualTo("abc123");
    }

    @Test
    void shouldRejectBlankTokenHash() {
        assertThatThrownBy(() -> InvitationTokenHash.ofHashedValue(" "))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("token hash");
    }

    @Test
    void shouldFreezeInvitationStatusEnum() {
        assertThat(InvitationStatus.values())
                .containsExactly(
                        InvitationStatus.PENDING,
                        InvitationStatus.ACCEPTED,
                        InvitationStatus.REVOKED,
                        InvitationStatus.EXPIRED
                );
    }
}
