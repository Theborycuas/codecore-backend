package com.codecore.audit.domain.valueobject;

import com.codecore.audit.domain.exception.InvalidDomainValueException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditValueObjectTest {

    @Test
    void actionCodeShouldAcceptValidCode() {
        assertThat(ActionCode.of("invitation.created").value()).isEqualTo("invitation.created");
    }

    @Test
    void actionCodeShouldTrimWhitespace() {
        assertThat(ActionCode.of("  invitation.accepted  ").value()).isEqualTo("invitation.accepted");
    }

    @Test
    void actionCodeShouldRejectBlank() {
        assertThatThrownBy(() -> ActionCode.of("   "))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void actionCodeShouldRejectTooLong() {
        String tooLong = "a".repeat(65);
        assertThatThrownBy(() -> ActionCode.of(tooLong))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("64");
    }

    @Test
    void actionCodeShouldAcceptMaxLength() {
        String max = "a".repeat(64);
        assertThat(ActionCode.of(max).value()).hasSize(64);
    }

    @Test
    void resourceTypeShouldRejectBlank() {
        assertThatThrownBy(() -> ResourceType.of(""))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void resourceTypeShouldRejectTooLong() {
        assertThatThrownBy(() -> ResourceType.of("x".repeat(65)))
                .isInstanceOf(InvalidDomainValueException.class)
                .hasMessageContaining("64");
    }

    @Test
    void resourceTypeShouldAcceptValid() {
        assertThat(ResourceType.of("invitation").value()).isEqualTo("invitation");
    }

    @Test
    void resourceIdShouldWrapUuid() {
        UUID uuid = UUID.randomUUID();
        assertThat(ResourceId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    void auditEntryIdAndTenantIdShouldGenerateDistinctValues() {
        assertThat(AuditEntryId.generate()).isNotEqualTo(AuditEntryId.generate());
        assertThat(TenantId.generate()).isNotEqualTo(TenantId.generate());
    }

    @Test
    void membershipIdShouldEqualByValue() {
        UUID uuid = UUID.randomUUID();
        assertThat(MembershipId.of(uuid)).isEqualTo(MembershipId.of(uuid));
    }

    @Test
    void auditOutcomeShouldHaveSuccessAndFailure() {
        assertThat(AuditOutcome.values()).containsExactly(AuditOutcome.SUCCESS, AuditOutcome.FAILURE);
    }
}
